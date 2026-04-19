import { useEffect, useState } from 'preact/hooks';
import { api } from './api';
import { Launcher } from './pages/Launcher';
import { Install } from './pages/Install';
import { Uninstall } from './pages/Uninstall';
import { Events } from './pages/Events';
import { Status } from './pages/Status';
import { Sandbox } from './pages/Sandbox';
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
	const connected = useHeartbeat();

	useEffect(() => {
		const onChange = () => setRoute(currentRoute());
		window.addEventListener('hashchange', onChange);
		api.mode().then((m) => setDemo(m.demo)).catch(() => {});
		return () => window.removeEventListener('hashchange', onChange);
	}, []);

	const navClass = (target: Route) => route === target ? 'active' : '';

	return (
		<div class="app">
			{!connected && (
				<div class="connection-lost-banner" role="alert">
					Connection to ParseBot admin service lost -- Attempting to Reconnect
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
				</nav>
			</header>
			<main>
				{route === Route.LAUNCHER && <Launcher />}
				{route === Route.INSTALL && <Install />}
				{route === Route.UNINSTALL && <Uninstall />}
				{route === Route.EVENTS && <Events />}
				{route === Route.SANDBOX && <Sandbox />}
				{route === Route.STATUS && <Status />}
			</main>
		</div>
	);
}
