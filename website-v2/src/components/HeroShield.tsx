import { useRef } from "react";

export function HeroShield() {
  const wrap = useRef<HTMLDivElement>(null);
  const onMove = (e: React.MouseEvent) => {
    const r = wrap.current?.getBoundingClientRect();
    if (!r) return;
    const el = wrap.current!;
    el.style.setProperty("--mx", String(((e.clientX - r.left) / r.width - 0.5) * 26));
    el.style.setProperty("--my", String(((e.clientY - r.top) / r.height - 0.5) * 26));
  };
  const reset = () => {
    const el = wrap.current;
    if (!el) return;
    el.style.setProperty("--mx", "0");
    el.style.setProperty("--my", "0");
  };
  return (
    <div ref={wrap} onMouseMove={onMove} onMouseLeave={reset} className="relative">
      <svg viewBox="0 0 420 420" shapeRendering="geometricPrecision" role="img" aria-label="درع حصن المتوهج فوق أوراق دافئة بأشكال زمردية" className="w-full h-auto">
        <defs>
          <linearGradient id="shieldFill" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="oklch(0.52 0.07 162)" />
            <stop offset="100%" stopColor="oklch(0.38 0.08 155)" />
          </linearGradient>
          <radialGradient id="glow" cx="50%" cy="42%" r="55%">
            <stop offset="0%" stopColor="oklch(0.47 0.075 160 / 0.35)" />
            <stop offset="100%" stopColor="oklch(0.47 0.075 160 / 0)" />
          </radialGradient>
        </defs>
        <g className="hero-parallax" style={{ transform: "translate(calc(var(--mx,0)*-0.6px), calc(var(--my,0)*-0.6px))" }}>
          <path d="M60 90 C 120 20, 300 10, 380 80 C 420 160, 410 300, 340 380 C 250 430, 110 420, 50 340 C 10 260, 20 150, 60 90 Z" fill="oklch(0.94 0.02 155)" opacity="0.7" />
          <path d="M90 70 C 180 -10, 340 40, 360 150 C 380 260, 320 360, 210 385 C 110 400, 40 330, 45 220 C 50 140, 60 100, 90 70 Z" fill="oklch(0.955 0.02 160)" />
          <circle cx="330" cy="100" r="46" fill="oklch(0.93 0.025 85)" opacity="0.8" />
          <circle cx="95" cy="330" r="30" fill="oklch(0.93 0.025 85)" opacity="0.6" />
        </g>
        <circle cx="210" cy="185" r="150" fill="url(#glow)" className="glow-pulse" />
        <g className="shield-float hero-parallax" transform="translate(110,115)" style={{ transform: "translate(calc(110px + var(--mx,0)*1px), calc(115px + var(--my,0)*1px))" }}>
          <path
            d="M40 190 V95 C40 45 65 15 100 15 C135 15 160 45 160 95 V190 H40 Z M96 60 H104 V190 H96 Z"
            fillRule="evenodd"
            fill="url(#shieldFill)"
            stroke="oklch(0.83 0.09 90)"
            strokeWidth="6"
            strokeLinejoin="round"
            vectorEffect="non-scaling-stroke"
          />
        </g>
      </svg>
    </div>
  );
}

export function LogoMark({ size = 36 }: { size?: number }) {
  return (
    <span className="gradient-emerald inline-flex items-center justify-center rounded-xl transition-transform duration-300 group-hover:rotate-6 group-hover:scale-105" style={{ width: size, height: size }}>
      <svg viewBox="0 0 200 200" width={size * 0.66} height={size * 0.66} aria-hidden="true" shapeRendering="geometricPrecision">
        <path d="M40 190 V95 C40 45 65 15 100 15 C135 15 160 45 160 95 V190 H40 Z M96 60 H104 V190 H96 Z" fillRule="evenodd" fill="oklch(0.985 0.01 95)" />
      </svg>
    </span>
  );
}
