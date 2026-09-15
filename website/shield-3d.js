// HISN 3D Shield — premium, calm, lazy, DPR capped, reduced-motion + WebGL fallback
// States: IDLE (slow rotate) → PROTECTED (glow) → THREAT (pulse) → BLOCKED (shake)
// Fallback: SVG hero-mark stays, canvas hidden. Mobile: lighter (no shadows, DPR 1)
(function(){
  const canvas = document.getElementById('shield-canvas');
  const svg = document.querySelector('.hero-mark');
  if(!canvas || !svg) return;
  if(window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
  if(!isWebGLAvailable()) return;

  // lazy load three r160
  const importMap = document.createElement('script');
  importMap.type = 'importmap';
  importMap.textContent = JSON.stringify({imports:{three:"https://unpkg.com/three@0.160.0/build/three.module.js"}});
  document.head.appendChild(importMap);

  // defer to idle
  const start = () => import('three').then(THREE => init(THREE)).catch(()=>{});
  if('requestIdleCallback' in window) requestIdleCallback(start, {timeout:2000}); else setTimeout(start, 800);

  function isWebGLAvailable(){
    try{ const c=document.createElement('canvas'); return !!(c.getContext('webgl')||c.getContext('experimental-webgl')); }catch(e){return false}
  }

  function init(THREE){
    const dpr = Math.min(window.devicePixelRatio||1, 1.5); // cap per #14
    const renderer = new THREE.WebGLRenderer({canvas, alpha:true, antialias: dpr<1.2});
    renderer.setPixelRatio(dpr);
    renderer.setSize(canvas.clientWidth, canvas.clientHeight, false);
    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(28, 1, 0.1, 100);
    camera.position.set(0,0,6);

    // lights — calm, not neon
    scene.add(new THREE.AmbientLight(0xF7F1E6, 0.9));
    const dir = new THREE.DirectionalLight(0xC9A15C, 0.6); dir.position.set(2,3,4); scene.add(dir);

    // shield geometry — simple extruded arch (low poly <2K verts)
    const shape = new THREE.Shape();
    shape.moveTo(-0.8,-1.2); shape.lineTo(-0.8,-0.1);
    shape.bezierCurveTo(-0.8,-0.7, -0.4,-1.1, 0,-1.1);
    shape.bezierCurveTo(0.4,-1.1, 0.8,-0.7, 0.8,-0.1);
    shape.lineTo(0.8,-1.2); shape.lineTo(-0.8,-1.2);
    const geo = new THREE.ExtrudeGeometry(shape,{depth:0.18, bevelEnabled:true, bevelThickness:0.02, bevelSize:0.02, bevelSegments:2});
    geo.center();
    const mat = new THREE.MeshStandardMaterial({color:0x7A9471, roughness:0.7, metalness:0.05});
    const shield = new THREE.Mesh(geo, mat);
    scene.add(shield);

    // slit
    const slitGeo = new THREE.BoxGeometry(0.07,1.0,0.2);
    const slitMat = new THREE.MeshStandardMaterial({color:0x1B1F3B});
    const slit = new THREE.Mesh(slitGeo, slitMat);
    slit.position.z = 0.12;
    shield.add(slit);

    // outline — LineLoop
    const points = shape.getPoints(20).map(p=> new THREE.Vector3(p.x,p.y,0.09));
    const lineGeo = new THREE.BufferGeometry().setFromPoints(points);
    const lineMat = new THREE.LineBasicMaterial({color:0xC9A15C});
    const outline = new THREE.LineLoop(lineGeo, lineMat);
    shield.add(outline);

    canvas.style.display='block'; svg.style.display='none';

    let state='IDLE', t=0, raf;
    function animate(){
      raf=requestAnimationFrame(animate);
      t+=0.01;
      if(state==='IDLE') shield.rotation.y = Math.sin(t*0.2)*0.15;
      if(state==='PROTECTED') shield.rotation.y +=0.003;
      if(state==='THREAT') shield.scale.setScalar(1+Math.sin(t*3)*0.015);
      if(state==='BLOCKED') shield.position.x = Math.sin(t*12)*0.03;
      renderer.render(scene,camera);
    }
    animate();

    // scroll storytelling: hero → demo → trust
    const onScroll=()=>{
      const y=window.scrollY, h=window.innerHeight;
      if(y < h*0.4) state='IDLE';
      else if(y < h*1.2) state='PROTECTED';
      else if(y < h*2.2) state='THREAT';
      else state='BLOCKED';
    };
    window.addEventListener('scroll', onScroll, {passive:true});

    // demo chips → trigger THREAT/BLOCKED
    document.querySelectorAll('.chip').forEach(c=>{
      c.addEventListener('click',()=>{
        const site=c.dataset.site;
        state = site==='unknown' ? 'BLOCKED' : 'PROTECTED';
        setTimeout(()=> state='IDLE', 1800);
      });
    });

    // DPR + resize
    window.addEventListener('resize',()=>{
      renderer.setSize(canvas.clientWidth, canvas.clientHeight, false);
      renderer.setPixelRatio(Math.min(window.devicePixelRatio||1,1.5));
      renderer.render(scene,camera);
    });

    // cleanup on page hide (WebGL context lost)
    document.addEventListener('visibilitychange',()=>{
      if(document.hidden) cancelAnimationFrame(raf); else animate();
    });

    // expose for QA
    window.__hisnShield = {scene, shield, setState:(s)=> state=s};
  }
})();
