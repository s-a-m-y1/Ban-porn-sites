import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";
import { Root } from "./routes/__root";
import Index from "./routes/index";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <Root>
      <Index />
    </Root>
  </StrictMode>,
);
