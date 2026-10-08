/**
 * 手机适配层（精简版，供独立预览）：把插件原版 UI 的左侧 228px 侧栏，
 * 在窄屏下改成「选择供应商」切换面板——照 dsh-phone.js 的 .dim-jh-layout 那段逻辑，
 * 去掉与本预览无关的部分（技能投喂、settings 嵌入等）。
 */
(() => {
  if (window.__jhPhoneAdapt) return;
  window.__jhPhoneAdapt = true;
  const media = window.matchMedia('(max-width: 700px)');
  document.body.classList.add('dsh-phone-mobile');

  const adapt = () => {
    document.querySelectorAll('.dim-jh-layout').forEach(layout => {
      const rail = layout.querySelector('.dim-jh-rail');
      if (!rail) return;
      let header = layout.querySelector('.dsh-phone-provider');
      if (!media.matches) {
        header?.remove();
        layout.classList.remove('dsh-phone-choosing');
        return;
      }
      const selected = rail.querySelector('.dim-jh-provider[aria-selected="true"]');
      if (!header) {
        header = document.createElement('div');
        header.className = 'dsh-phone-provider';
        const title = document.createElement('span');
        title.className = 'dsh-phone-selected';
        const toggle = document.createElement('button');
        toggle.className = 'dim-jh-btn';
        toggle.type = 'button';
        toggle.addEventListener('click', () => {
          layout.classList.toggle('dsh-phone-choosing');
          toggle.setAttribute('aria-expanded', String(layout.classList.contains('dsh-phone-choosing')));
          toggle.textContent = layout.classList.contains('dsh-phone-choosing') ? '返回账号' : '选择供应商';
        });
        header.append(title, toggle);
        layout.insertBefore(header, rail);
        rail.addEventListener('click', event => {
          if (!event.target.closest('.dim-jh-provider')) return;
          layout.classList.remove('dsh-phone-choosing');
          toggle.setAttribute('aria-expanded', 'false');
          toggle.textContent = '选择供应商';
        });
        layout.classList.add('dsh-phone-choosing');
        toggle.setAttribute('aria-expanded', 'true');
        toggle.textContent = '返回账号';
      }
      const label = selected?.querySelector('.dim-jh-providerLabel strong')?.textContent || '供应商账号';
      if (header.dataset.selected !== label) {
        const title = header.querySelector('.dsh-phone-selected');
        title.replaceChildren();
        const icon = selected?.querySelector('.dim-jh-providerIcon');
        if (icon) title.append(icon.cloneNode(true));
        title.append(document.createTextNode(label));
        header.dataset.selected = label;
      }
    });
  };

  let pending = false;
  const queue = () => {
    if (pending) return;
    pending = true;
    requestAnimationFrame(() => { pending = false; adapt(); });
  };
  new MutationObserver(queue).observe(document.body, { childList: true, subtree: true, attributes: true, attributeFilter: ['aria-selected', 'title'] });
  media.addEventListener('change', queue);
  window.addEventListener('resize', queue);
  adapt();
})();
