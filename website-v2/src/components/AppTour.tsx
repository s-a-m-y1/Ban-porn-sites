import { useEffect, useRef, useState } from "react";

function PhoneTilt({ children }: { children: React.ReactNode }) {
  const ref = useRef<HTMLDivElement>(null);
  const onMove = (e: React.MouseEvent) => {
    if (window.innerWidth < 861) return;
    const r = ref.current?.getBoundingClientRect();
    if (!r) return;
    const x = (e.clientX - r.left) / r.width - 0.5;
    const y = (e.clientY - r.top) / r.height - 0.5;
    ref.current!.style.transform = `perspective(900px) rotateY(${x * 8}deg) rotateX(${-y * 5}deg)`;
  };
  const reset = () => { if (ref.current) ref.current.style.transform = "perspective(900px) rotateY(0) rotateX(0)"; };
  return (
    <div ref={ref} onMouseMove={onMove} onMouseLeave={reset} className="transition-transform duration-300 will-change-transform">
      {children}
    </div>
  );
}

const TOUR_MS = 5200;

const screens = [
  {
    key: "home",
    tab: "الرئيسية",
    title: "الرئيسية",
    blurb: "حالة الحماية أمامك: البوابة، حديث اليوم، وعدّاد المحاولات.",
  },
  { key: "stats", tab: "الإحصائيات", title: "الإحصائيات", blurb: "أرقامك تبقى عندك: اليوم، الأسبوع، وخريطة النشاط." },
  { key: "protect", tab: "الحماية", title: "ميزات الحماية", blurb: "فئات بيدك: إباحي، قمار، وقائمة مواقع يدوية." },
  { key: "support", tab: "الدعم", title: "ادعم حصن", blurb: "مشروع وقفي — بدعمك يستمر ويتطوّر." },
  { key: "settings", tab: "الإعدادات", title: "الإعدادات", blurb: "تحكم كامل: اللغة، الوضع الليلي، PIN، والدمج مع النظام." },
];

function Toggle({ on }: { on: boolean }) {
  return (
    <span className={`inline-flex h-5 w-9 items-center rounded-full px-0.5 ${on ? "bg-primary" : "bg-[#EDE4D2]"}`}>
      <span className={`h-4 w-4 rounded-full bg-white shadow transition-transform ${on ? "-translate-x-0" : "-translate-x-4"}`} />
    </span>
  );
}

function Row({ children, i }: { children: React.ReactNode; i: number }) {
  return (
    <div className="tour-rise mx-3 mb-2 flex items-center justify-between rounded-2xl border border-[#E2E2D8] bg-white px-3 py-2.5 text-[#22333B]" style={{ animationDelay: `${i * 70}ms` }}>
      {children}
    </div>
  );
}

function HomeScreen() {
  return (
    <div>
      <div className="tour-rise flex items-center justify-between px-4 pt-4" style={{ animationDelay: "0ms" }}>
        <div>
          <p className="text-[13px] text-[#6E7A72]">أسعد الله يومك</p>
          <p className="text-[11px] text-[#6E7A72]">اليوم ٢ من رحلتك</p>
        </div>
        <span className="rounded-full border border-[#E2E2D8] bg-[#EAF3EE] px-3 py-1 text-[10px] text-[#2E6B52]">مجاناً</span>
      </div>
      <div className="tour-rise mt-4 flex flex-col items-center" style={{ animationDelay: "90ms" }}>
        <p className="flex items-center gap-1.5 text-[12px] font-medium text-[#22333B]">
          <span className="h-1.5 w-1.5 rounded-full bg-primary" />
          البوابة مغلقة
        </p>
        <div className="tour-ring mt-3 grid h-24 w-24 place-items-center rounded-full gradient-emerald shadow-lift">
          <svg viewBox="0 0 200 200" className="h-12 w-12" aria-hidden="true">
            <path d="M40 190 V95 C40 45 65 15 100 15 C135 15 160 45 160 95 V190 H40 Z" fill="none" stroke="oklch(0.985 0.01 95)" strokeWidth="10" />
            <rect x="96" y="55" width="8" height="130" fill="oklch(0.985 0.01 95)" />
          </svg>
        </div>
        <p className="mt-2 text-[10px] text-[#6E7A72]">اضغط لفتح البوابة — يتطلب PIN</p>
      </div>
      <div className="tour-rise mx-3 mt-4 rounded-2xl bg-[#EDE4D2] p-3" style={{ animationDelay: "180ms" }}>
        <p className="text-[10px] font-bold text-[#22333B]">خُطوة إلى النور</p>
        <p className="mt-1 text-[11px] leading-5 text-[#182634]">«التقوى هاهنا» — وأشار إلى صدره ثلاث مرات</p>
      </div>
      <div className="tour-rise mx-3 mt-3 grid grid-cols-2 gap-2" style={{ animationDelay: "260ms" }}>
        <div className="rounded-2xl border border-[#E2E2D8] bg-white p-2.5 text-center">
          <p className="text-[15px] font-extrabold text-[#22333B]">٨٤</p>
          <p className="text-[9px] text-[#6E7A72]">محاولة محجوبة</p>
        </div>
        <div className="rounded-2xl border border-[#E2E2D8] bg-white p-2.5 text-center">
          <p className="text-[15px] font-extrabold text-[#22333B]">٧٦٧٧١</p>
          <p className="text-[9px] text-[#6E7A72]">دومين في القائمة</p>
        </div>
      </div>
    </div>
  );
}

function StatsScreen() {
  const week = [
    { d: "س", v: 45 }, { d: "ح", v: 72 }, { d: "ن", v: 30 },
    { d: "ث", v: 88 }, { d: "ر", v: 60 }, { d: "خ", v: 20 }, { d: "ج", v: 55 },
  ];
  return (
    <div className="px-3 pt-4">
      <p className="tour-rise text-[13px] font-extrabold text-[#22333B]" style={{ animationDelay: "0ms" }}>الإحصائيات</p>
      <div className="mt-3 grid grid-cols-2 gap-2">
        <div className="tour-rise rounded-2xl border border-[#E2E2D8] bg-white p-3" style={{ animationDelay: "70ms" }}>
          <p className="text-[10px] text-[#6E7A72]">محاولات محجوبة — اليوم</p>
          <p className="text-[20px] font-extrabold text-[#C0392B]">٨٤</p>
        </div>
        <div className="tour-rise rounded-2xl border border-[#E2E2D8] bg-white p-3" style={{ animationDelay: "140ms" }}>
          <p className="text-[10px] text-[#6E7A72]">المتوسط اليومي</p>
          <p className="text-[20px] font-extrabold text-[#22333B]">١٢</p>
        </div>
      </div>
      <div className="tour-rise mt-3 rounded-2xl border border-[#E2E2D8] bg-white p-3" style={{ animationDelay: "210ms" }}>
        <p className="text-[10px] font-bold text-[#22333B]">هذا الأسبوع</p>
        <div className="mt-2 flex h-16 items-end justify-between gap-1.5">
          {week.map((b, i) => (
            <div key={i} className="flex flex-1 flex-col items-center gap-1">
              <div className="tour-bar w-full rounded-t-md gradient-emerald" style={{ height: `${b.v}%`, animationDelay: `${260 + i * 60}ms` }} />
              <span className="text-[8px] text-[#6E7A72]">{b.d}</span>
            </div>
          ))}
        </div>
      </div>
      <div className="tour-rise mt-3 rounded-2xl border border-[#E2E2D8] bg-white p-3" style={{ animationDelay: "480ms" }}>
        <p className="text-[10px] font-bold text-[#22333B]">الأكثر حظراً</p>
        {[["pornhub.com", "٤١"], ["xvideos.com", "١٨"], ["888.com", "٩"]].map(([s, n], i) => (
          <div key={s} className="mt-1.5 flex justify-between text-[10px]">
            <span className="text-[#6E7A72]" dir="ltr">{s}</span>
            <span className="font-bold">{n}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

function ProtectScreen() {
  return (
    <div className="pt-4">
      <p className="tour-rise mx-4 text-[13px] font-extrabold text-[#22333B]" style={{ animationDelay: "0ms" }}>ميزات الحماية</p>
      <div className="mt-3">
        <Row i={1}><span className="text-[11px]">حظر المواقع الإباحية</span><Toggle on /></Row>
        <Row i={2}><span className="text-[11px]">حظر مواقع القمار</span><Toggle on /></Row>
        <Row i={3}><span className="text-[11px]">حظر مواقع الأخبار الكاذبة</span><Toggle on={false} /></Row>
        <Row i={4}><span className="text-[11px]">حظر مواقع الفيروسات</span><Toggle on={false} /></Row>
        <Row i={5}>
          <span className="text-[11px]">مواقع محظورة يدوياً</span>
          <span className="rounded-full bg-[#EAF3EE] px-2 py-0.5 text-[10px] text-[#2E6B52]">+٤</span>
        </Row>
        <Row i={6}>
          <span className="text-[11px]">التطبيقات المحظورة</span>
          <span className="rounded-full bg-[#EDE4D2] px-2 py-0.5 text-[10px]">X (٢)</span>
        </Row>
      </div>
    </div>
  );
}

function SupportScreen() {
  return (
    <div className="flex flex-col items-center px-4 pt-8 text-center">
      <div className="tour-float grid h-16 w-16 place-items-center rounded-3xl gradient-emerald shadow-lift">
        <svg viewBox="0 0 24 24" className="h-8 w-8" fill="oklch(0.985 0.01 95)" aria-hidden="true">
          <path d="M12 21s-7.5-4.9-9.8-9.2C.6 8.9 2.3 5 6 5c2.2 0 3.6 1.2 6 3.6C14.4 6.2 15.8 5 18 5c3.7 0 5.4 3.9 3.8 6.8C19.5 16.1 12 21 12 21z" />
        </svg>
      </div>
      <p className="tour-rise mt-4 text-[13px] font-extrabold text-[#22333B]" style={{ animationDelay: "80ms" }}>ادعم حصن</p>
      <p className="tour-rise mt-2 text-[11px] leading-5 text-[#6E7A72]" style={{ animationDelay: "150ms" }}>
        حصن مشروع وقفي لحماية الأسرة.<br />دعمك يساعدنا على الاستمرار والتطوير.
      </p>
      <p className="tour-rise mt-3 text-[10px] text-[#6E7A72]" dir="ltr" style={{ animationDelay: "220ms" }}>sam858y@gmail.com</p>
      <p className="tour-rise mt-3 rounded-full bg-[#EDE4D2] px-3 py-1 text-[9px] text-[#6E7A72]" style={{ animationDelay: "290ms" }}>
        Paymob • Fawry • فودافون كاش — قريباً
      </p>
      <button className="tour-rise mt-4 rounded-full gradient-emerald px-5 py-2 text-[11px] font-bold text-[#F4F1EA] shadow-lift" style={{ animationDelay: "360ms" }}>
        تواصل عبر البريد
      </button>
    </div>
  );
}

function SettingsScreen() {
  return (
    <div className="pt-4">
      <p className="tour-rise mx-4 text-[13px] font-extrabold text-[#22333B]" style={{ animationDelay: "0ms" }}>الإعدادات</p>
      <div className="mt-3">
        <Row i={1}><span className="text-[11px]">اللغة</span><span className="text-[10px] text-[#6E7A72]">العربية</span></Row>
        <Row i={2}><span className="text-[11px]">الوضع الليلي</span><Toggle on={false} /></Row>
        <Row i={3}><span className="text-[11px]">حماية برقم سري</span><Toggle on /></Row>
        <Row i={4}><span className="text-[11px]">مسؤول الجهاز (ضد الحذف)</span><Toggle on /></Row>
        <Row i={5}><span className="text-[11px]">التشغيل مع الجهاز</span><Toggle on /></Row>
        <Row i={6}><span className="text-[11px]">VPN الدائم</span><Toggle on={false} /></Row>
      </div>
      <p className="mt-3 text-center text-[9px] text-[#6E7A72]">حصن ١.٠ — كل البيانات على جهازك</p>
    </div>
  );
}

const renderScreen: Record<string, () => JSX.Element> = {
  home: HomeScreen,
  stats: StatsScreen,
  protect: ProtectScreen,
  support: SupportScreen,
  settings: SettingsScreen,
};

export function AppTour() {
  const [i, setI] = useState(0);
  const [playing, setPlaying] = useState(true);
  const [progress, setProgress] = useState(0);
  const raf = useRef(0);
  const start = useRef(0);

  useEffect(() => {
    if (!playing) return;
    start.current = performance.now() - (progress / 100) * TOUR_MS;
    const tick = (t: number) => {
      const p = ((t - start.current) / TOUR_MS) * 100;
      if (p >= 100) {
        setProgress(0);
        setI((v) => (v + 1) % screens.length);
        start.current = t;
      } else {
        setProgress(p);
      }
      raf.current = requestAnimationFrame(tick);
    };
    raf.current = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf.current);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [playing, i]);

  const go = (n: number) => {
    setProgress(0);
    setI((n + screens.length) % screens.length);
  };

  const Active = renderScreen[screens[i].key];

  return (
    <section id="tour" className="mx-auto max-w-6xl px-4 py-16">
      <h2 className="text-center text-3xl font-extrabold text-ink">جولة داخل التطبيق</h2>
      <p className="mx-auto mt-3 max-w-xl text-center text-[#6E7A72]">خمس شاشات حقيقية من حصن — كما هي على جهازك، تماماً.</p>

      <div className="mt-10 grid items-center gap-10 lg:grid-cols-2">
        <div className="mx-auto w-[290px] max-w-full">
          <PhoneTilt>
          <div className="rounded-[2.6rem] bg-[#16242F] p-3 shadow-lift">
            <div className="flex h-[560px] flex-col overflow-hidden rounded-[2rem] bg-[#F7F5EF]">
              <div className="relative flex-1 overflow-hidden">
                <div key={screens[i].key} className="tour-stage absolute inset-0">
                  <div className="tour-screen-in absolute inset-0">
                    <Active />
                  </div>
                </div>
              </div>
              <div className="flex w-full items-center justify-around border-t border-[#E2E2D8] bg-white px-1 py-1.5">
              {screens.map((s, k) => (
                <button
                  key={s.key}
                  onClick={() => go(k)}
                  aria-label={s.tab}
                  className={`flex min-w-0 flex-1 select-none flex-col items-center gap-0.5 rounded-xl px-1 py-1 text-[8px] transition-colors ${k === i ? "text-[#1E5631]" : "text-[#6B7280]"}`}
                >
                  <span className={`grid h-6 w-11 place-items-center rounded-full ${k === i ? "bg-[#EAF3EE]" : ""}`}>
                    {k === 0 && <svg viewBox="0 0 24 24" className="h-4 w-4" fill="currentColor"><path d="M12 3 3 10v11h6v-7h6v7h6V10z"/></svg>}
                    {k === 1 && <svg viewBox="0 0 24 24" className="h-4 w-4" fill="currentColor"><path d="M4 20V10h4v10H4zm6 0V4h4v16h-4zm6 0v-7h4v7h-4z"/></svg>}
                    {k === 2 && <svg viewBox="0 0 24 24" className="h-4 w-4" fill="currentColor"><path d="M12 2 4 5v6c0 5 3.4 9.7 8 11 4.6-1.3 8-6 8-11V5z"/></svg>}
                    {k === 3 && <svg viewBox="0 0 24 24" className="h-4 w-4" fill="currentColor"><path d="M12 21s-7.5-4.9-9.8-9.2C.6 8.9 2.3 5 6 5c2.2 0 3.6 1.2 6 3.6C14.4 6.2 15.8 5 18 5c3.7 0 5.4 3.9 3.8 6.8C19.5 16.1 12 21 12 21z"/></svg>}
                    {k === 4 && <svg viewBox="0 0 24 24" className="h-4 w-4" fill="currentColor"><path d="M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8zm9 4-2.1-1.7.3-2.7-2.6-.8-1.3-2.3-2.6.6L12 3.6 9.3 5.1l-2.6-.6-1.3 2.3-2.6.8.3 2.7L3 12l2.1 1.7-.3 2.7 2.6.8 1.3 2.3 2.6-.6L12 20.4l2.7-1.5 2.6.6 1.3-2.3 2.6-.8-.3-2.7z" fill="none" stroke="currentColor" strokeWidth="2"/></svg>}
                  </span>
                  {s.tab}
                </button>
              ))}
              </div>
            </div>
          </div>
          </PhoneTilt>
        </div>

        <div className="grid gap-3">
          {screens.map((s, k) => (
            <button
              key={s.key}
              onClick={() => go(k)}
              className={`surface-card relative select-none overflow-hidden px-5 py-4 text-right transition-all ${k === i ? "border-primary shadow-lift -translate-y-1" : "hover:-translate-y-0.5"}`}
            >
              <p className={`text-sm font-bold ${k === i ? "text-primary" : "text-ink"}`}>{s.tab} — {s.title}</p>
              <p className="mt-1 text-[13px] text-[#6E7A72]">{s.blurb}</p>
              {k === i && playing && (
                <span className="absolute inset-x-0 bottom-0 h-1 bg-mint">
                  <span className="block h-full gradient-emerald" style={{ width: `${progress}%` }} />
                </span>
              )}
            </button>
          ))}
          <div className="mt-2 flex items-center justify-center gap-3">
            <button onClick={() => go(i - 1)} aria-label="السابق" className="grid h-10 w-10 place-items-center rounded-full border border-border bg-card text-ink transition-colors hover:bg-mint">‹</button>
            <button
              onClick={() => setPlaying((p) => !p)}
              className="rounded-full gradient-emerald px-6 py-2.5 text-sm font-bold text-primary-foreground shadow-lift transition-transform hover:-translate-y-0.5"
            >
              {playing ? "إيقاف مؤقت" : "تشغيل"}
            </button>
            <button onClick={() => go(i + 1)} aria-label="التالي" className="grid h-10 w-10 place-items-center rounded-full border border-border bg-card text-ink transition-colors hover:bg-mint">›</button>
          </div>
        </div>
      </div>
    </section>
  );
}
