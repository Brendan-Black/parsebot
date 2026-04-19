import { Route, routeHref } from '../consts';

export function Launcher() {
  return (
    <div class="launcher">
      <a href={routeHref(Route.INSTALL)}>
        <strong>Install</strong>
        <span>Configure and register the ParseBot service.</span>
      </a>
      <a href={routeHref(Route.UNINSTALL)}>
        <strong>Uninstall</strong>
        <span>Stop and remove the ParseBot service.</span>
      </a>
      <a href={routeHref(Route.EVENTS)}>
        <strong>Events</strong>
        <span>View events emitted by the service.</span>
      </a>
      <a href={routeHref(Route.SANDBOX)}>
        <strong>Sandbox</strong>
        <span>Send a PDF through Claude interactively.</span>
      </a>
      <a href={routeHref(Route.STATUS)}>
        <strong>Status</strong>
        <span>Check whether the service is running.</span>
      </a>
    </div>
  );
}
