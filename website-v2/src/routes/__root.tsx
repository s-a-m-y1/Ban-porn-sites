import type { ReactNode } from "react";

export function Root({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen bg-background text-foreground font-sans gradient-paper">
      <div className="bg-blobs" aria-hidden="true">
        <span className="blob blob-a" />
        <span className="blob blob-b" />
      </div>
      {children}
    </div>
  );
}
