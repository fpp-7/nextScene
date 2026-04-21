import { RouterProvider } from "react-router";
import { router } from "./routes";

export default function App() {
  return (
    <div className="max-w-md mx-auto min-h-screen bg-background">
      <RouterProvider router={router} />
    </div>
  );
}
