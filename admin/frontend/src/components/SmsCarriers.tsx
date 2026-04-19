import { Carrier } from '../api';

export function SmsCarriers({ carriers }: { carriers: Carrier[] }) {
  return (
    <details>
      <summary>SMS carrier gateway reference</summary>
      <table>
        <thead>
          <tr><th>Gateway</th><th>Carrier</th></tr>
        </thead>
        <tbody>
          {carriers.map((c) => (
            <tr key={c.gateway}><td>{c.gateway}</td><td>{c.carrier}</td></tr>
          ))}
        </tbody>
      </table>
    </details>
  );
}
