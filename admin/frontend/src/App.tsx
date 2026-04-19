import { useEffect, useState } from 'preact/hooks';
import { Launcher } from './pages/Launcher';
import { Install } from './pages/Install';
import { Uninstall } from './pages/Uninstall';
import { Events } from './pages/Events';
import { Status } from './pages/Status';
import { TestParse } from './pages/TestParse';
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
	useHeartbeat();

	useEffect(() => {
		const onChange = () => setRoute(currentRoute());
		window.addEventListener('hashchange', onChange);
		return () => window.removeEventListener('hashchange', onChange);
	}, []);

	const navClass = (target: Route) => route === target ? 'active' : '';

	return (
		<div class="app">
			<header class="header">
				<h1>ParseBot Admin</h1>
				<nav class="nav">
					<a href={routeHref(Route.LAUNCHER)} class={navClass(Route.LAUNCHER)}>Home</a>
					<a href={routeHref(Route.INSTALL)} class={navClass(Route.INSTALL)}>Install</a>
					<a href={routeHref(Route.EVENTS)} class={navClass(Route.EVENTS)}>Events</a>
					<a href={routeHref(Route.TEST_PARSE)} class={navClass(Route.TEST_PARSE)}>Test Parse</a>
					<a href={routeHref(Route.STATUS)} class={navClass(Route.STATUS)}>Status</a>
				</nav>
			</header>
			<main>
				{route === Route.LAUNCHER && <Launcher />}
				{route === Route.INSTALL && <Install />}
				{route === Route.UNINSTALL && <Uninstall />}
				{route === Route.EVENTS && <Events />}
				{route === Route.TEST_PARSE && <TestParse />}
				{route === Route.STATUS && <Status />}
			</main>
		</div>
	);
}
