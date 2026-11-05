/* Launcher chrome adapter for Home Base v22, commit 774171c.
 * Runs only on the exact Home Base origin. No page -> Android bridge exists here.
 * Uses the site's own React actions; never writes or fabricates Notion records.
 */
if (!window.freqSiteNavigate) {
  window.freqSiteNavigate = function (target) {
    if (location.origin !== 'https://farhan-home-base.farhaansadeeq.chatgpt.site') return 'origin';
    if (!document.querySelector('.layout-picker') && !document.querySelector('.fos')) return 'waiting';
    if (!document.querySelector('.fos-dock')) {
      if (!window.dispatchEvent(new Event('farhan-layout-before-change', {cancelable: true}))) return 'blocked';
      try {localStorage.setItem('farhan-os-layout', 'freq');} catch (_) {}
      document.documentElement.dataset.layout = 'freq';
      window.dispatchEvent(new CustomEvent('farhan-layout-change', {detail: 'freq'}));
      return 'waiting';
    }
    var dock = document.querySelector('.fos-dock');
    if (!dock) return 'waiting';
    if (!document.getElementById('freq-native-chrome')) {
      var style = document.createElement('style');style.id = 'freq-native-chrome';
      style.textContent = '.fos-dock{display:none!important}.fos{padding-bottom:20px!important}';
      document.head.appendChild(style);
    }
    var labels = {Home: 'The Grid', Work: 'Operations', Studio: 'Noise Machine', Personal: 'Headspace', Cyberdeck: 'Nethack', Files: 'The Vault', Settings: 'System Lab'};
    var name = target === 'graph' || target === 'area' ? 'Home' : target;
    if (target && target !== 'graph' && document.querySelector('.knowledge-sheet[data-state="open"]')) {
      document.dispatchEvent(new KeyboardEvent('keydown', {key: 'Escape', code: 'Escape', bubbles: true}));
      return 'waiting';
    }
    if (labels[name]) {
      var nav = dock.querySelector('button[aria-label="' + labels[name] + '"]');
      if (!nav) return 'waiting';
      if (nav.getAttribute('aria-current') !== 'page') {nav.click();return 'waiting';}
    }
    if (target === 'graph') window.dispatchEvent(new Event('home-open-pinboard'));
    if (target === 'area') {
      var pin = document.querySelector('.area-edge-tab');
      if (!pin) return 'waiting';
      if (pin.getAttribute('aria-expanded') !== 'true') {pin.click();return 'waiting';}
      var tree = document.getElementById('grid-area-tree');
      if (!tree) return 'waiting';
      tree.scrollIntoView({block: 'start', behavior: 'smooth'});
    }
    return 'ready';
  };
}
