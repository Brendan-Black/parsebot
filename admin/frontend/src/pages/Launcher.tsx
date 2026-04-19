export function Launcher() {
  return (
    <div class="launcher">
      <a href="#/install">
        <strong>Install</strong>
        <span>Configure and register the ParseBot service.</span>
      </a>
      <a href="#/uninstall">
        <strong>Uninstall</strong>
        <span>Stop and remove the ParseBot service.</span>
      </a>
      <a href="#/events">
        <strong>Events</strong>
        <span>View events emitted by the service.</span>
      </a>
      <a href="#/test-parse">
        <strong>Test Parse</strong>
        <span>Send a PDF through Claude interactively.</span>
      </a>
      <a href="#/status">
        <strong>Status</strong>
        <span>Check whether the service is running.</span>
      </a>
    </div>
  );
}
