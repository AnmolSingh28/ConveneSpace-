export default function EmptyState({ icon: Icon, title, description, action }) {
  return (
    <div className="text-center py-16 px-4">
      <div className="inline-flex items-center justify-center w-14 h-14 bg-muted rounded-full mb-4">
        <Icon className="h-7 w-7 text-muted-foreground" />
      </div>
      <h3 className="text-lg font-semibold mb-1">{title}</h3>
      <p className="text-sm text-muted-foreground max-w-sm mx-auto mb-4">
        {description}
      </p>
      {action && action}
    </div>
  );
}