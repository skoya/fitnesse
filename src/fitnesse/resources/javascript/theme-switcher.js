(function () {
  function getCookie(name) {
    var value = document.cookie || '';
    var parts = value.split(';');
    for (var i = 0; i < parts.length; i++) {
      var pair = parts[i].trim().split('=');
      if (pair.length === 2 && pair[0] === name) {
        return pair[1];
      }
    }
    return '';
  }

  function setCookie(name, value, maxAgeSeconds) {
    var cookie = name + '=' + value + '; path=/';
    if (maxAgeSeconds) {
      cookie += '; max-age=' + maxAgeSeconds;
    }
    document.cookie = cookie;
  }

  function init() {
    var select = document.getElementById('theme-select');
    if (!select) {
      return;
    }
    var current = getCookie('fitnesse_theme');
    if (!current) {
      current = document.body.getAttribute('data-theme') || '';
    }
    if (current) {
      select.value = current;
    }
    select.addEventListener('change', function () {
      var next = select.value || '';
      if (!next) {
        return;
      }
      setCookie('fitnesse_theme', next, 60 * 60 * 24 * 365);
      window.location.reload();
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
