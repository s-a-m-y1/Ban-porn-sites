import type { ReactNode } from "react";

export function Root({ children }: { children: ReactNode }) {
  return <div className="min-h-screen bg-background text-foreground font-sans gradient-paper">{children}</div>;
}
