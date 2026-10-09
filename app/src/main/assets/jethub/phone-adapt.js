/**
 * 手机适配层（融合版 2026-10-10）：布局改由 jet-phone.css 承担——左侧 228px
 * 供应商侧栏在窄屏下变成页面顶部的「换行胶囊网格」（图标+名称，一行 3~4 个
 * 自动换行，全部同屏可见），下方直接显示选中供应商的账号面板。
 *
 * 本脚本只剩两件事：
 * 1. 给 body 打 dsh-phone-mobile 标记（jet-phone.css 的选择器靠它生效）；
 * 2. 兼容旧滚动逻辑：换行布局下 rail 不再是滚动容器（overflow-x:hidden），
 *    adapt() 的滚动居中在 flex-wrap 下自然无操作（scrollWidth==clientWidth），
 *    保留代码以兼容上游改回横滚布局。
 * 不做任何 DOM 移动/插入——全部样式覆盖都由 CSS 完成，React 无感。
 */
(() => {
  if (window.__jhPhoneAdapt) return;
  window.__jhPhoneAdapt = true;
  const media = window.matchMedia('(max-width: 700px)');
  document.body.classList.add('dsh-phone-mobile');

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
