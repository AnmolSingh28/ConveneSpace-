import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs) {
  return twMerge(clsx(inputs));
}

export function formatDate(dateString) {
  if (!dateString) return '';
  let date;
  if (typeof dateString === 'string' && dateString.match(/^\d{2}-\d{2}-\d{4}/)) {
    const [datePart, timePart] = dateString.split(' ');
    const [day, month, year] = datePart.split('-');
    const isoString = `${year}-${month}-${day}T${timePart || '00:00:00'}`;
    date = new Date(isoString);
  } else {
    date = new Date(dateString);
  }
  if (isNaN(date.getTime())) return '';
  return date.toLocaleDateString('en-IN', {
    weekday: 'short',
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}
export function formatTime(dateString) {
  if (!dateString) return '';
  let date;
  if (typeof dateString === 'string' && dateString.match(/^\d{2}-\d{2}-\d{4}/)) {
    const [datePart, timePart] = dateString.split(' ');
    const [day, month, year] = datePart.split('-');
    const isoString = `${year}-${month}-${day}T${timePart || '00:00:00'}`;
    date = new Date(isoString);
  } else {
  
    const str = typeof dateString === 'string' && !dateString.endsWith('Z') && !dateString.includes('+')
      ? dateString + 'Z'
      : dateString;
    date = new Date(str);
  }
  if (isNaN(date.getTime())) return '';
  return date.toLocaleTimeString('en-IN', {
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function formatDateTime(dateString) {
  if (!dateString) return '';
  return `${formatDate(dateString)} at ${formatTime(dateString)}`;
}

export function formatCurrency(amount) {
  if (!amount) return '₹0';
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(amount);
}

export function generateIdempotencyKey() {
  return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}