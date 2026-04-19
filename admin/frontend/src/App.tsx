import { useEffect, useState } from 'preact/hooks';
import { api } from './api';
import { Launcher } from './pages/Launcher';
import { Install } from './pages/Install';
import { Uninstall } from './pages/Uninstall';
import { Events } from './pages/Events';
import { Status } from './pages/Status';
import { Sandbox } from './pages/Sandbox';
import { Updates } from './pages/Updates';
import { useHeartbeat } from './heartbeat';
import { LEGACY_UI_ROUTE, Route, isRoute, routeHref } from './consts';

function currentRoute(): Route {
	const raw = window.location.hash.replace(/^#\/?/, '') || Route.LAUNCHER;
	const head = raw.split('/')[0];
	if (head === LEGACY_UI_ROUTE) return Route.LAUNCHER;
	if (isRoute(head)) return head;
	return Route.LAUNCHER;
}

export function App() {
	const [route, setRoute] = useState<Route>(currentRoute());
	const [demo, setDemo] = useState(false);
	const [dismissed, setDismissed] = useState(false);
	const connected = useHeartbeat();

	useEffect(() => {
		const onChange = () => setRoute(currentRoute());
		window.addEventListener('hashchange', onChange);
		api.mode().then((m) => setDemo(m.demo)).catch(() => {});
		return () => window.removeEventListener('hashchange', onChange);
	}, []);

	useEffect(() => {
		if (connected) setDismissed(false);
	}, [connected]);

	const navClass = (target: Route) => route === target ? 'active' : '';

	return (
		<div class="app">
			{!connected && !dismissed && (
				<div class="connection-lost-overlay" role="alert">
					<div class="connection-lost-modal">
						<div class="connection-lost-message">Connection to ParseBot admin service lost</div>
						<div class="connection-lost-actions">
							<button type="button" class="connection-lost-close" onClick={() => window.close()}>
								Close this tab
							</button>
							<button type="button" class="connection-lost-dismiss" onClick={() => setDismissed(true)}>
								Dismiss
							</button>
						</div>
					</div>
				</div>
			)}
			{!connected && dismissed && (
				<div class="connection-lost-readonly" role="alert">
					Connection lost — no changes can be saved
				</div>
			)}
			{demo && (
				<div class="demo-banner">
					DEMO MODE — all data is placeholder.
				</div>
			)}
			<header class="header">
				<h1>ParseBot Admin{demo ? ' (demo)' : ''}</h1>
				<nav class="nav">
					<a href={routeHref(Route.LAUNCHER)} class={navClass(Route.LAUNCHER)}>Home</a>
					<a href={routeHref(Route.INSTALL)} class={navClass(Route.INSTALL)}>Install</a>
					<a href={routeHref(Route.EVENTS)} class={navClass(Route.EVENTS)}>Events</a>
					<a href={routeHref(Route.SANDBOX)} class={navClass(Route.SANDBOX)}>Sandbox</a>
					<a href={routeHref(Route.STATUS)} class={navClass(Route.STATUS)}>Status</a>
					<a href={routeHref(Route.UPDATES)} class={navClass(Route.UPDATES)}>Updates</a>
				</nav>
			</header>
			<main>
				{route === Route.LAUNCHER && <Launcher />}
				{route === Route.INSTALL && <Install />}
				{route === Route.UNINSTALL && <Uninstall />}
				{route === Route.EVENTS && <Events />}
				{route === Route.SANDBOX && <Sandbox />}
				{route === Route.STATUS && <Status />}
				{route === Route.UPDATES && <Updates />}
			</main>
		</div>
	);
}
