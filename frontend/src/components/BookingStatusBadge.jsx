const STATUS_CONFIG = {
  PENDING: { label: 'Pending Payment', className: 'bg-amber-100 text-amber-800 border-amber-200' },
  CONFIRMED: { label: 'Confirmed', className: 'bg-green-100 text-green-800 border-green-200' },
  CANCELLED: { label: 'Cancelled', className: 'bg-red-100 text-red-800 border-red-200' },
  REFUND_INITIATED: { label: 'Refund Initiated', className: 'bg-blue-100 text-blue-800 border-blue-200' },
  REFUNDED: { label: 'Refunded', className: 'bg-purple-100 text-purple-800 border-purple-200' },
  ATTENDED: { label: 'Attended', className: 'bg-teal-100 text-teal-800 border-teal-200' }, 
};

export default function BookingStatusBadge({ status }) {
  const config = STATUS_CONFIG[status] || { label: status, className: 'bg-gray-100 text-gray-800' };
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${config.className}`}>
      {config.label}
    </span>
  );
}