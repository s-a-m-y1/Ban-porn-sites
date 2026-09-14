# T3 — Android unit tests (JUnit)

## TASK ID
T3-android-tests

## STATUS
DONE

## WHAT WAS IMPLEMENTED
Created the app's first unit-test source set (android/app/src/test did not
exist) with three pure-JVM JUnit 4 test classes covering the app's testable
core logic — the DNS packet filter path, the blocklist matcher, and the PIN
validator. No production files were modified and build.gradle.kts was not
touched (the existing junit:junit:4.13.2 testImplementation is all these
tests need).

- DnsPacketParserTest (39 tests): packet classification (isDnsPacket),
  domain extraction (extractQueryDomain), payload location helpers, and
  response construction (buildNxDomainResponse for blocked domains,
  buildDnsResponse for allowed ones). Every test hand-builds a real
  IPv4/UDP/DNS query byte-by-byte with an in-test builder, and the
  response tests verify the IPv4 checksum with an independent one's-
  complement verifier, not a re-run of the production algorithm.
- BlocklistIndexTest (14 tests): isBlocked exact match in category and
  custom sets, parent-domain walk (subdomains blocked, suffix-spoofing
  like "example.com.evil.net" NOT blocked), case-insensitivity, trailing
  dot handling, and single-label termination. The index's private
  @Volatile backing sets are seeded via reflection so load(context) —
  the only Android-dependent path — never runs.
- PinManagerTest (24 tests): all five validation rules (length 4–6,
  digits-only, all-same-digit, ascending/descending sequences, common
  PIN list) plus rule-ordering expectations ("1234" reports errSeq,
  "0000"/"1111" report errSame).

Total: 77 tests, 3 classes, all green.

## FILES CHANGED
- /home/sami/App-bloking-sex/android/app/src/test/java/com/contentfilter/app/DnsPacketParserTest.kt (new)
- /home/sami/App-bloking-sex/android/app/src/test/java/com/contentfilter/app/BlocklistIndexTest.kt (new)
- /home/sami/App-bloking-sex/android/app/src/test/java/com/contentfilter/app/PinManagerTest.kt (new)

No production files modified. build.gradle.kts not modified.

## IMPORTANT DECISIONS
1. Pure-JVM only, by design: the project has no Robolectric and no
   Mockito in its test dependencies, so classes needing an Android
   Context, Room DB, or Retrofit (StatsTracker, AppPrefs,
   BlocklistRepository, Categories, BlocklistIndex.load) were excluded.
   This follows the task scope instruction exactly.
2. BlocklistIndex.isBlocked tested via reflection seeding of its private
   backing sets (blockedByCategory / customBlocked) rather than going
   through load(context). Kotlin object fields may compile static or
   instance, so the seeding helper handles both via Modifier.isStatic.
3. R.string.pin_err_* ints are compared directly — they are plain int
   constants in the compiled R class and safe to reference from JVM
   tests without any android.jar method calls.
4. Byte-level packet builders live in the test file and are independent
   of the production code: this makes checksum validity assertions
   meaningful (an independent verifier, not a copy of the code under
   test).
5. Tests assert existing production behavior, including rule ordering
   and walk semantics, rather than an idealized spec — so they document
   the system as built. The interesting behaviors are pinned in tests
   and called out under KNOWN ISSUES below.

## TESTS RUN
Command (from /home/sami/App-bloking-sex/android):
  ./gradlew testDebugUnitTest --console=plain
Result: BUILD SUCCESSFUL in 31s.

Per-class counts (from app/build/test-results/testDebugUnitTest/*.xml):
  com.contentfilter.app.DnsPacketParserTest  — 39 tests, 0 failures, 0 errors, 0 skipped
  com.contentfilter.app.BlocklistIndexTest   — 14 tests, 0 failures, 0 errors, 0 skipped
  com.contentfilter.app.PinManagerTest       — 24 tests, 0 failures, 0 errors, 0 skipped
  TOTAL: 77 tests, 0 failures, 0 errors, 0 skipped

## TEST RESULTS
All 77 tests pass. Exit code 0. BUILD SUCCESSFUL in 31s (26 tasks, 5
executed, 21 up-to-date — the app's main sources compiled clean, so the
test source set built without touching any pre-existing outputs).

## KNOWN ISSUES
Production observations pinned by tests (behavior as-built, not bugs fixed
— per task rules these were only noted):
1. BlocklistIndex parent-domain walk: isBlocked walks up one label at a
   time, so if a public suffix itself is ever listed (e.g. "co.uk"),
   EVERY domain under it is blocked ("site.co.uk" blocked by the
   "co.uk" entry). Suffix-spoofing is safe in the other direction:
   "example.com.evil.net" does NOT match a listed "example.com" because
   the walk strips leftmost labels only. (test: parentWalkReachesTopLevelLabels)
2. PinManager rule ordering: "1234" reports errSeq (sequence check runs
   before the common-list check) even though "1234" is not on the
   implementation's common list; "0000"/"1111" report errSame rather
   than errCommon for the same reason. Error-id precedence is
   length > digits > same > sequence > common. (test: commonPins0000And1111AreCaughtByEarlierRules)
3. DnsPacketParser.buildDnsResponse hardcodes the resolver source IP
   10.111.0.2 — matching FilterVpnService's TUN address, but a
   behavioral coupling worth knowing if the VPN subnet ever changes.
   (test: dnsResponseIsAddressedBackToTheClient)
4. extractQueryDomain rejects any label length byte ≥ 0x40, which also
   rejects compression pointers (0xC0). Legitimate DNS *queries* never
   contain compression pointers in the QNAME, so this is conservative
   and correct. (test: returnsNullOnCompressionPointerInQname)
5. nxResponseDoesNotMutateTheQuery documents that buildNxDomainResponse
   operates on a copy — important because the VPN pump reuses its read
   buffer.
6. Environment note: repeated Bash-classifier outages blocked the
   Gradle run for ~45 minutes of wall-clock retries this session; the
   run itself then succeeded on first attempt with no test fixes
   needed — the suite was green as written.

## NEXT DEPENDENCIES
None for T3 — the task is self-contained and complete. For future tasks:
- T-covering-gaps: StatsTracker, AppPrefs, BlocklistRepository, and
  BlocklistIndex.load need Robolectric (or instrumentation tests) for
  their Context/Room/Retrofit paths; adding the Robolectric testImplementation
  to app/build.gradle.kts would unlock them.
- The 77 green tests gate any refactor of the DNS hot path
  (FilterVpnService.pumpPackets, DnsPacketParser, BlocklistIndex.isBlocked)
  and of PinManager.validate.
