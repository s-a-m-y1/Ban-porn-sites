import { HeroShield, LogoMark } from "../components/HeroShield";
import { AppTour } from "../components/AppTour";

function ThemeToggle() {
  const toggle = () => {
    const el = document.documentElement;
    const dark = el.classList.toggle("dark");
    try { localStorage.setItem("hisn-theme", dark ? "dark" : "light"); } catch {}
  };
  return (
    <button
      onClick={toggle}
      aria-label="تبديل الوضع الليلي"
      className="grid h-10 w-10 place-items-center rounded-full border border-border bg-card text-ink transition-all hover:-translate-y-0.5 hover:border-primary/40 hover:shadow-soft"
    >
      <svg viewBox="0 0 24 24" className="h-5 w-5 dark:hidden" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
        <circle cx="12" cy="12" r="4" />
        <path d="M12 2v2m0 16v2M4.9 4.9l1.4 1.4m11.4 11.4 1.4 1.4M2 12h2m16 0h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
      </svg>
      <svg viewBox="0 0 24 24" className="hidden h-5 w-5 dark:block" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" />
      </svg>
    </button>
  );
}

const features = [
  { icon: "M12 2 4 5v6c0 5 3.4 9.7 8 11 4.6-1.3 8-6 8-11V5z", t: "حجب لحظي", d: "فحص DNS داخل جهازك — المحتوى المحجوب لا يظهر إطلاقاً." },
  { icon: "M4 20V10h4v10H4zm6 0V4h4v16h-4zm6 0v-7h4v7h-4z", t: "إحصائيات وتقدم", d: "محاولات، أيام، وخريطة نشاط — كلها تبقى على جهازك." },
  { icon: "M12 2a5 5 0 0 0-5 5v3H6v12h12V10h-1V7a5 5 0 0 0-5-5zm2 8h-4V7a2 2 0 1 1 4 0z", t: "حماية برقم سري", d: "PIN مشفّر بـ PBKDF2 يحمي الإعدادات ووقف الحماية." },
  { icon: "M12 2a10 10 0 1 0 10 10h-3a7 7 0 1 1-7-7zm1 4v4l4 2-1 2-6-3V6z", t: "جدولة الحماية", d: "تعمل وتتوقف تلقائياً في الأوقات التي تختارها." },
  { icon: "M4 4h16v4H4zm0 6h16v10H4zm3 3v4h4v-4z", t: "قائمة مواقع يدوية", d: "أضف أو احذف أي موقع بنفسك — قائمتك تُحترم فوراً." },
  { icon: "M6 2h12v6l-4 4 4 10H6l4-10-4-4z", t: "حماية من الحذف", d: "مسؤول جهاز + قفل تطبيقات يجعلان التجاوز صعباً." },
];

const steps = [
  { t: "حمّل وثبّت", d: "٩.٢ ميجابايت — يعمل على أندرويد ٧ فما فوق، بلا حساب وبلا إعلانات." },
  { t: "امنح إذن VPN", d: "فحص واحد داخل جهازك — لا خادم خارجي ولا تتبع إطلاقاً." },
  { t: "أغلق البوابة", d: "الحماية تعمل في الخلفية، وترى تقدمك يوماً بيوم." },
];

export default function Index() {
  return (
    <>
      <header className="sticky top-0 z-50 border-b border-border/60 bg-background/85 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
          <a href="#top" className="group flex items-center gap-2">
            <LogoMark />
            <span className="text-xl font-extrabold text-ink">حصن</span>
          </a>
          <nav className="hidden items-center gap-7 text-sm text-muted-foreground md:flex">
            <a href="#features" className="nav-link transition-colors hover:text-primary">المميزات</a>
            <a href="#how" className="nav-link transition-colors hover:text-primary">كيف يعمل</a>
            <a href="#support" className="nav-link transition-colors hover:text-primary">ادعم حصن</a>
          </nav>
          <div className="flex items-center gap-3">
            <ThemeToggle />
            <a href="#download" className="btn-sheen rounded-full gradient-emerald px-5 py-2.5 text-sm font-bold text-primary-foreground shadow-lift transition-transform hover:-translate-y-0.5">
              حمّل التطبيق
            </a>
          </div>
        </div>
      </header>

      <main id="top">
        <section className="mx-auto grid max-w-6xl items-center gap-10 px-4 pb-16 pt-14 md:grid-cols-2 md:pt-20">
          <div>
            <h1 className="text-4xl font-extrabold leading-[1.35] text-ink md:text-5xl">
              بوابة واحدة تُغلق<br />الطريق أمام الضرر
            </h1>
            <p className="mt-5 max-w-lg leading-8 text-muted-foreground">
              حصن يفلّر الإنترنت داخل جهازك نفسه: المواقع التي تحتاجها تفتح كالمعتاد، وما لا يليق يُحجب قبل أن يصل إلى الشاشة — بلا سجلات، بلا مراقبة.
            </p>
            <div className="mt-8 flex flex-wrap items-center gap-4">
              <a href="downloads/hisn-v1.0.apk" download className="btn-sheen rounded-full gradient-emerald px-8 py-3.5 font-bold text-primary-foreground shadow-lift transition-transform hover:-translate-y-1">
                حمّل التطبيق
              </a>
              <a href="#tour" className="text-sm font-bold text-primary underline decoration-primary/40 underline-offset-8 transition-colors hover:decoration-primary">
                شاهد جولة التطبيق
              </a>
            </div>
            <div className="mt-10 grid max-w-md grid-cols-3 gap-3">
              <div className="surface-card px-3 py-4 text-center transition-all duration-300 hover:-translate-y-1 hover:shadow-lift">
                <p className="text-2xl font-extrabold text-ink">٧٦٧٧١</p>
                <p className="mt-1 text-[11px] text-muted-foreground">دومين في قائمة الحجب</p>
              </div>
              <div className="surface-card px-3 py-4 text-center transition-all duration-300 hover:-translate-y-1 hover:shadow-lift">
                <p className="text-2xl font-extrabold text-ink">٨٤</p>
                <p className="mt-1 text-[11px] text-muted-foreground">محاولة محجوبة اليوم</p>
              </div>
              <div className="surface-card px-3 py-4 text-center transition-all duration-300 hover:-translate-y-1 hover:shadow-lift">
                <p className="text-2xl font-extrabold text-ink">٤</p>
                <p className="mt-1 text-[11px] text-muted-foreground">فئات حماية</p>
              </div>
            </div>
          </div>
          <div className="surface-card rounded-[2rem] p-2 md:order-last">
            <HeroShield />
          </div>
        </section>

        <section className="gradient-emerald py-12 text-center">
          <p className="mx-auto max-w-3xl px-4 text-xl font-bold leading-[2.2] text-primary-foreground md:text-2xl">
            «مَن تركَ شيئاً للهِ، عوَّضَه اللهُ خيراً منه»
          </p>
          <p className="mt-3 text-sm text-primary-foreground/70">قال ﷺ</p>
        </section>

        <AppTour />

        <section id="features" className="mx-auto max-w-6xl px-4 py-16">
          <h2 className="text-center text-3xl font-extrabold text-ink">المميزات</h2>
          <p className="mx-auto mt-3 max-w-xl text-center text-muted-foreground">كل ما تحتاجه لحماية تركيزك — موجود، ومختبر على أجهزة حقيقية.</p>
          <div className="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {features.map((f) => (
              <article key={f.t} className="surface-card group p-6 transition-all duration-300 hover:-translate-y-1 hover:border-primary/40 hover:shadow-lift">
                <span className="grid h-12 w-12 place-items-center rounded-2xl bg-mint transition-transform duration-300 group-hover:scale-110 group-hover:rotate-3">
                  <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="oklch(0.47 0.075 160)" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <path d={f.icon} />
                  </svg>
                </span>
                <h3 className="mt-4 font-extrabold text-ink">{f.t}</h3>
                <p className="mt-2 text-sm leading-7 text-muted-foreground">{f.d}</p>
              </article>
            ))}
          </div>
        </section>

        <section id="how" className="mx-auto max-w-4xl px-4 py-16">
          <h2 className="text-center text-3xl font-extrabold text-ink">كيف يعمل؟</h2>
          <div className="surface-card mt-10 grid gap-8 p-8 sm:grid-cols-3 sm:gap-4">
            {steps.map((s, i) => (
              <div key={s.t} className="text-center">
                <span className="gradient-emerald mx-auto grid h-12 w-12 place-items-center rounded-full text-lg font-extrabold text-primary-foreground shadow-lift">
                  {["١", "٢", "٣"][i]}
                </span>
                <h3 className="mt-4 font-extrabold text-ink">{s.t}</h3>
                <p className="mt-2 text-sm leading-7 text-muted-foreground">{s.d}</p>
              </div>
            ))}
          </div>
        </section>

        <section id="download" className="mx-auto max-w-6xl px-4 py-16">
          <div className="gradient-emerald rounded-[2rem] px-6 py-14 text-center shadow-lift md:px-12">
            <h2 className="text-3xl font-extrabold text-primary-foreground">ابدأ حماية جهازك الآن</h2>
            <p className="mx-auto mt-4 max-w-xl leading-8 text-primary-foreground/85">
              حزمة واحدة ٩.٢ ميجابايت — تعمل على أندرويد ٧+، ولا تحتاج حساباً ولا اتصالاً بخادم.
            </p>
            <div className="mt-8 flex flex-wrap items-center justify-center gap-4">
              <a href="downloads/hisn-v1.0.apk" download className="rounded-full bg-card px-8 py-3.5 font-bold text-ink shadow-soft transition-transform hover:-translate-y-1">
                حمّل التطبيق — ٩.٢ MB
              </a>
              <a href="mailto:sam858y@gmail.com?subject=Support%20HISN" id="support" className="rounded-full border border-primary-foreground/30 px-8 py-3.5 font-bold text-primary-foreground transition-colors hover:bg-primary-foreground/10">
                ادعم حصن
              </a>
            </div>
            <p className="mt-6 text-[11px] text-primary-foreground/60" dir="ltr">SHA256: 79eda473dba08843b3eeb1beb4c5f61837ce251773688165d15561b3065746ac</p>
          </div>
        </section>
      </main>

      <footer className="border-t border-border/60 py-8 text-center">
        <p className="text-sm text-muted-foreground">حصن — ما لا يليق لا يصل.</p>
      </footer>
    </>
  );
}
