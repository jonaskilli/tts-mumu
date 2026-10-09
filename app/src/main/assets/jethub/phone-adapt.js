/**
 * 手机适配层（融合版 2026-10-10）：布局改由 jet-phone.css 承担——左侧 228px
 * 供应商侧栏在窄屏下变成页面顶部的「换行胶囊网格」（图标+名称，一行 3~4 个
 * 自动换行，全部同屏可见），下方直接显示选中供应商的账号面板。
 *
 * 选中收起（10-10 用户令「选中后只显示当前供应商和账号」）：
 * 1. 用户点选供应商 → body 打 jh-collapse，CSS 把网格收成一行
 *    （只留选中胶囊 + 「切换 ▾」）；初载不收（保持全览）。
 * 2. 点「切换 ▾」（rail 空白区）或当前选中胶囊 → 摘掉 jh-collapse 展开网格。
 * 收起/展开纯 class 切换，DOM 一个节点不动，React 无感。
 * 不做任何 DOM 移动/插入——全部样式覆盖都由 CSS 完成。
 */
(() => {
  if (window.__jhPhoneAdapt) return;
  window.__jhPhoneAdapt = true;
  const media = window.matchMedia('(max-width: 700px)');
  document.body.classList.add('dsh-phone-mobile');

  // 点选供应商 → 收起网格（事件委托到 rail，React 重渲染不丢监听）。
  // ⚠️ 不能在点击瞬间立即收（10-10 真机报障「切换有时卡住」）：React 把
  // aria-selected 更新到新 chip 慢一拍，立即 collapse 会把被点的 chip
  // （aria-selected 还是 false）藏掉、旧 chip 留守——用户看到「点了没反应」。
  // 修法：点击后只记下意图（pendingCollapse + 目标），等 MutationObserver
  // 观察到 aria-selected 真正切到该 chip 再收；600ms 兜底（React 始终没跟上
  // 就放弃收起，保持展开至少能再点，绝不出现「藏错人」）。
  let pendingCollapseTarget = null;
  let pendingCollapseTimer = 0;
  const tryCollapse = () => {
    if (!pendingCollapseTarget) return;
    const chip = document.querySelector('.dim-jh-provider[data-provider="' + pendingCollapseTarget + '"]');
    // 目标 chip 已是选中态（或已从 DOM 消失=列表重建中）才收
    if (!chip || chip.getAttribute('aria-selected') === 'true') {
      document.body.classList.add('jh-collapse');
      clearPendingCollapse();
    }
  };
  const clearPendingCollapse = () => {
    pendingCollapseTarget = null;
    if (pendingCollapseTimer) { clearTimeout(pendingCollapseTimer); pendingCollapseTimer = 0; }
  };
  document.addEventListener('click', (e) => {
    if (!media.matches) return;
    const rail = e.target.closest && e.target.closest('.dim-jh-rail');
    if (!rail) return;
    const chip = e.target.closest('.dim-jh-provider');
    if (chip && chip.getAttribute('aria-selected') !== 'true') {
      const row = chip.closest('[data-provider]');
      pendingCollapseTarget = row ? row.getAttribute('data-provider') : null;
      if (pendingCollapseTarget) {
        // 立即试一次（React 快时无感），慢时交给 observer/兜底
        tryCollapse();
        if (pendingCollapseTarget) {
          clearTimeout(pendingCollapseTimer);
          pendingCollapseTimer = setTimeout(clearPendingCollapse, 600);
        }
      }
    } else if (!chip || chip.getAttribute('aria-selected') === 'true') {
      // 点「切换 ▾」（rail 空白）或已选中的胶囊：展开网格
      clearPendingCollapse();
      document.body.classList.remove('jh-collapse');
    }
  }, true);

  // 页头「关闭」按钮打隐藏标记（10-10 用户令）：它是桌面 DSH 的退出口，
  // 手机端返回键已接管。识别依据：页头按钮里唯一不带 title 的（其余按钮
  // 全部有 hover 说明）。React 重渲染会重建按钮节点，MutationObserver 里补打。
  const markCloseBtn = () => {
    if (!media.matches) return;
    document.querySelectorAll('.dim-jh-header .dim-jh-btn').forEach((b) => {
      if (!b.hasAttribute('title') && b.textContent.trim() === '关闭') {
        b.classList.add('jh-hideClose');
      }
    });
  };
  markCloseBtn();
  new MutationObserver(() => { markCloseBtn(); tryCollapse(); }).observe(document.body, { childList: true, subtree: true });

  const adapt = () => {
    if (!media.matches) return;
    const layout = document.querySelector('.dim-jh-layout');
    if (!layout) return;
    const rail = layout.querySelector('.dim-jh-rail');
    if (!rail) return;
    const selected = rail.querySelector('.dim-jh-provider[aria-selected="true"]');
    if (!selected) return;
    // scrollIntoView 会连带滚动页面纵向，改手动算横向偏移。
    // 用 rect 差值算（宽条的 offsetParent 因 display:contents 不一定是 rail）
    const railRect = rail.getBoundingClientRect();
    const selRect = selected.getBoundingClientRect();
    const target = rail.scrollLeft + (selRect.left - railRect.left)
      - (rail.clientWidth - selRect.width) / 2;
    const max = rail.scrollWidth - rail.clientWidth;
    const left = Math.max(0, Math.min(target, max));
    if (Math.abs(rail.scrollLeft - left) > 1) {
      rail.scrollLeft = left;
      // React 重挂载/重渲染可能在本帧之后再重置 scrollLeft：延迟二次校验。
      // ⚠️ 兜底用 rail.scrollTo（只滚 rail 自己）——scrollIntoView 会把
      // 页面视口一起横向滚走（实测 rail 的 rect.left 变 -623，页头被滚出视野）。
      setTimeout(() => {
        if (Math.abs(rail.scrollLeft - left) > 1) {
          rail.scrollTo({ left, behavior: 'instant' });
        }
      }, 120);
    }
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
