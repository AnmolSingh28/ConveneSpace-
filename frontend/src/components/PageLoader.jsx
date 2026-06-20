import { Loader2 } from "lucide-react";

export default function PageLoader({ message = "Loading..." }) {
  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] w-full space-y-4">
      <Loader2 className="h-12 w-12 text-primary animate-spin" />
      <p className="text-lg font-medium text-muted-foreground animate-pulse">
        {message}
      </p>
    </div>
  );
}