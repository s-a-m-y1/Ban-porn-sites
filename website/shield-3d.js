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
    scene.add(new THREE.AmbientLight(0xF7F5EF, 0.9));
    const dir = new THREE.DirectionalLight(0xCDA55C, 0.6); dir.position.set(2,3,4); scene.add(dir);

    // shield geometry — fortress arch (low poly <2K verts)
    const shape = new THREE.Shape();
    shape.moveTo(-0.8,-1.2); shape.lineTo(-0.8,-0.1);
    shape.bezierCurveTo(-0.8,-0.7, -0.4,-1.1, 0,-1.1);
    shape.bezierCurveTo(0.4,-1.1, 0.8,-0.7, 0.8,-0.1);
    shape.lineTo(0.8,-1.2); shape.lineTo(-0.8,-1.2);
    const geo = new THREE.ExtrudeGeometry(shape,{depth:0.18, bevelEnabled:true, bevelThickness:0.02, bevelSize:0.02, bevelSegments:2});
    geo.center();
    const mat = new THREE.MeshStandardMaterial({color:0x2F7A6B, roughness:0.7, metalness:0.05});
    const shield = new THREE.Mesh(geo, mat);
    scene.add(shield);
    // protective sphere — glowing, per reference hero RIGHT
    const sphereGeo = new THREE.SphereGeometry(1.15, 32, 32);
    const sphereMat = new THREE.MeshStandardMaterial({color:0x2F7A6B, transparent:true, opacity:0.08, roughness:0.3, metalness:0.1, emissive:0x16242F, emissiveIntensity:0.15});
    const sphere = new THREE.Mesh(sphereGeo, sphereMat);
    sphere.scale.set(1,1.15,1);
    scene.add(sphere);
    const sphereWire = new THREE.LineSegments(new THREE.WireframeGeometry(sphereGeo), new THREE.LineBasicMaterial({color:0xCDA55C, transparent:true, opacity:0.12}));
    sphereWire.scale.copy(sphere.scale);
    scene.add(sphereWire);
    // fortress walls — two side bastions (calm, not gaming)
    const wallGeo = new THREE.BoxGeometry(0.18,0.9,0.12);
    const wallMat = new THREE.MeshStandardMaterial({color:0x22344A, roughness:0.9});
    const leftWall = new THREE.Mesh(wallGeo, wallMat); leftWall.position.set(-0.92,-0.15, -0.05); scene.add(leftWall);
    const rightWall = new THREE.Mesh(wallGeo, wallMat); rightWall.position.set(0.92,-0.15, -0.05); scene.add(rightWall);
    // ground — subtle
    const groundGeo = new THREE.PlaneGeometry(6,2);
    const groundMat = new THREE.MeshStandardMaterial({color:0x16242F, roughness:1});
    const ground = new THREE.Mesh(groundGeo, groundMat); ground.rotation.x = -Math.PI/2; ground.position.y = -1.3; scene.add(ground);
    // particles — glowing dust (low count on mobile)
    const isMobile = window.innerWidth < 720;
    const pCount = isMobile ? 30 : 80;
    const pGeo = new THREE.BufferGeometry();
    const pPos = new Float32Array(pCount*3);
    for(let i=0;i<pCount;i++){ pPos[i*3]= (Math.random()-0.5)*3; pPos[i*3+1]= (Math.random()-0.5)*2; pPos[i*3+2]= (Math.random()-0.5)*1; }
    pGeo.setAttribute('position', new THREE.BufferAttribute(pPos,3));
    const pMat = new THREE.PointsMaterial({color:0xCDA55C, size:0.015, transparent:true, opacity:0.6, depthWrite:false});
    const particles = new THREE.Points(pGeo, pMat); scene.add(particles);
    scene.fog = new THREE.Fog(0x16242F, 4, 8);

    // slit
    const slitGeo = new THREE.BoxGeometry(0.07,1.0,0.2);
    const slitMat = new THREE.MeshStandardMaterial({color:0x16242F});
    const slit = new THREE.Mesh(slitGeo, slitMat);
    slit.position.z = 0.12;
    shield.add(slit);

    // outline — LineLoop
    const points = shape.getPoints(20).map(p=> new THREE.Vector3(p.x,p.y,0.09));
    const lineGeo = new THREE.BufferGeometry().setFromPoints(points);
    const lineMat = new THREE.LineBasicMaterial({color:0xCDA55C});
    const outline = new THREE.LineLoop(lineGeo, lineMat);
    shield.add(outline);

    canvas.style.display='block'; svg.style.display='none';

    let state='IDLE', t=0, raf;
    function animate(){
      raf=requestAnimationFrame(animate);
      t+=0.01;
      if(state==='IDLE'){ shield.rotation.y = Math.sin(t*0.2)*0.15; sphere.rotation.y = -t*0.05; particles.rotation.y = t*0.02; }
      if(state==='PROTECTED'){ shield.rotation.y +=0.003; sphere.rotation.y +=0.001; particles.material.opacity = 0.7; }
      if(state==='THREAT'){ shield.scale.setScalar(1+Math.sin(t*3)*0.015); sphere.scale.setScalar(1+Math.sin(t*3)*0.01); particles.material.opacity = 0.9; }
      if(state==='BLOCKED'){ shield.position.x = Math.sin(t*12)*0.03; sphere.material.opacity = 0.15; particles.material.opacity = 0.3; } else sphere.material.opacity = 0.08;
      particles.rotation.y +=0.0005;
      // mouse tilt
      shield.rotation.x = mouseY * 0.15;
      shield.rotation.y += mouseX * 0.01;
      sphere.rotation.x = mouseY * 0.08;
      renderer.render(scene,camera);
    }
    let mouseX=0, mouseY=0;
    window.addEventListener('mousemove', e=>{
      mouseX = (e.clientX / window.innerWidth - 0.5)*0.5;
      mouseY = (e.clientY / window.innerHeight - 0.5)*0.5;
    }, {passive:true});
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
