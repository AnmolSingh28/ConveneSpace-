import { useState } from 'react';
import { Button } from './ui/button';
import { Input } from './ui/input';

export default function SeatMapBuilder({ onSave }) {
  const [section, setSection] = useState({ name: '', rows: 10, seatsPerRow: 20 });

  return (
    <div className="p-6 border rounded-2xl bg-card space-y-4">
      <h3 className="font-bold">Dynamic Layout Generator</h3>
      <div className="grid grid-cols-3 gap-4">
        <Input placeholder="Section Name (e.g. VIP)" value={section.name} 
          onChange={e => setSection({...section, name: e.target.value})} />
        <Input type="number" placeholder="Rows" value={section.rows} 
          onChange={e => setSection({...section, rows: parseInt(e.target.value)})} />
        <Input type="number" placeholder="Seats per Row" value={section.seatsPerRow} 
          onChange={e => setSection({...section, seatsPerRow: parseInt(e.target.value)})} />
      </div>
      <Button className="w-full" onClick={() => onSave(section)}>
        Generate {section.rows * section.seatsPerRow} Seats
      </Button>
    </div>
  );
}