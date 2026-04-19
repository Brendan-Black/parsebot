import { useEffect, useState } from 'preact/hooks';
import { Launcher } from './pages/Launcher';
import { Install } from './pages/Install';
import { Uninstall } from './pages/Uninstall';
import { Events } from './pages/Events';
import { Status } from './pages/Status';
import { TestParse } from './pages/TestParse';
import { useHeartbeat } from './heartbeat';

type Route = 'launcher' | 'install' | 'uninstall' | 'events' | 'status' | 'test-parse';

function currentRoute(): Route {
  const raw = window.location.hash.replace(/^#\/?/, '') || 'launcher';
  const head = raw.split('/')[0];
  if (head === 'ui') return 'launcher';
  if (head === 'install' || head === 'uninstall' || head === 'events'
      || head === 'status' || head === 'launcher' || head === 'test-parse') {
    return head;
  }
  return 'launcher';
}

export function App() {
  const [route, setRoute] = useState<Route>(currentRoute());
  useHeartbeat();

  useEffect(() => {
    const onChange = () => setRoute(currentRoute());
    window.addEventListener('hashchange', onChange);
    return () => window.removeEventListener('hashchange', onChange);
  }, []);

  return (
    <div class="app">
      <header class="header">
        <h1>ParseBot Admin</h1>
        <nav class="nav">
          <a href="#/launcher" class={route === 'launcher' ? 'active' : ''}>Home</a>
          <a href="#/install" class={route === 'install' ? 'active' : ''}>Install</a>
          <a href="#/events" class={route === 'events' ? 'active' : ''}>Events</a>
          <a href="#/test-parse" class={route === 'test-parse' ? 'active' : ''}>Test Parse</a>
          <a href="#/status" class={route === 'status' ? 'active' : ''}>Status</a>
        </nav>
      </header>
      <main>
        {route === 'launcher' && <Launcher />}
        {route === 'install' && <Install />}
        {route === 'uninstall' && <Uninstall />}
        {route === 'events' && <Events />}
        {route === 'test-parse' && <TestParse />}
        {route === 'status' && <Status />}
      </main>
    </div>
  );
}
