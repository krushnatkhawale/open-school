import * as THREE from 'three';
import { SVGLoader } from 'three/addons/loaders/SVGLoader.js';

var STATE_KEY = 'bg-theme';

var NAVY = 0x2c5282;
var GLOW = 0x8fb4dd;
var TILE = 420;
var BASE_HEIGHT = 960;
var DRIFT_X = 1.4;
var DRIFT_Y = 2.4;
var ROCK_MAX = 0.022;

var renderer, scene, camera, clock;
var tiles = [];
var shapeDefs = [];
var running = false;
var animId = null;
var ready = false;
var resizeTimer = null;

function viewportWorld() {
  var h = window.innerHeight || 1;
  var w = window.innerWidth || 1;
  return { w: BASE_HEIGHT * (w / h), h: BASE_HEIGHT };
}

function fitCamera(world) {
  camera = new THREE.OrthographicCamera(0, world.w, 0, world.h, -100, 100);
}

function init() {
  var canvas = document.getElementById('three-bg');
  if (!canvas) return;

  renderer = new THREE.WebGLRenderer({ canvas: canvas, alpha: true, antialias: true });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  renderer.setSize(window.innerWidth, window.innerHeight);

  scene = new THREE.Scene();
  clock = new THREE.Clock();

  fitCamera(viewportWorld());
  loadSVG();

  window.addEventListener('resize', onResize);
}

function onResize() {
  renderer.setSize(window.innerWidth, window.innerHeight);
  clearTimeout(resizeTimer);
  resizeTimer = setTimeout(function () {
    var world = viewportWorld();
    fitCamera(world);
    rebuildGrid(world);
  }, 120);
}

function tileCounts(world) {
  var cols = Math.ceil(world.w / TILE) + 1;
  var rows = Math.ceil(world.h / TILE) + 1;
  return { cols: cols, rows: rows };
}

function rebuildGrid(world) {
  for (var i = 0; i < tiles.length; i++) scene.remove(tiles[i]);
  tiles = [];
  buildGrid(world);
  if (running) tick();
}

function buildGrid(world) {
  var counts = tileCounts(world);
  var meshes = 0;

  for (var row = 0; row < counts.rows; row++) {
    for (var col = 0; col < counts.cols; col++) {
      var ox = col * TILE;
      var oy = row * TILE;
      var tg = new THREE.Group();
      tg.position.set(ox, oy, 0);
      tg.userData.baseX = ox;
      tg.userData.baseY = oy;
      tg.userData.phase = (row * counts.cols + col) * 1.37;
      tg.userData.items = [];

      for (var s = 0; s < shapeDefs.length; s++) {
        var def = shapeDefs[s];
        var baseMat = new THREE.MeshBasicMaterial({
          color: NAVY,
          transparent: true,
          opacity: 0.12,
          side: THREE.DoubleSide,
          depthWrite: false
        });
        var glowMat = new THREE.MeshBasicMaterial({
          color: GLOW,
          transparent: true,
          opacity: 0.12,
          side: THREE.DoubleSide,
          depthWrite: false,
          blending: THREE.AdditiveBlending
        });
        var baseMesh = new THREE.Mesh(def.geom, baseMat);
        var glowMesh = new THREE.Mesh(def.glow, glowMat);
        glowMesh.position.set(def.cx, def.cy, 0.5);
        glowMesh.scale.set(1.35, 1.35, 1);
        tg.add(baseMesh);
        tg.add(glowMesh);
        tg.userData.items.push(baseMat, glowMat);
        meshes += 2;
      }

      scene.add(tg);
      tiles.push(tg);
    }
  }

  window.__themeAnimated = {
    tiles: tiles.length,
    meshes: meshes,
    cols: counts.cols,
    rows: counts.rows,
    world: world
  };
}

function loadSVG() {
  var loader = new SVGLoader();
  loader.load('/images/school-objects.svg', function (data) {
    shapeDefs = [];
    data.paths.forEach(function (pathData) {
      var shapes = pathData.toShapes(true);
      shapes.forEach(function (shape) {
        var points = shape.getPoints(0);
        if (points.length === 0) return;
        var minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
        for (var i = 0; i < points.length; i++) {
          var p = points[i];
          if (p.x < minX) minX = p.x;
          if (p.x > maxX) maxX = p.x;
          if (p.y < minY) minY = p.y;
          if (p.y > maxY) maxY = p.y;
        }
        var geom = new THREE.ShapeGeometry(shape);
        var glow = geom.clone();
        glow.center();
        shapeDefs.push({
          geom: geom,
          glow: glow,
          cx: (minX + maxX) / 2,
          cy: (minY + maxY) / 2
        });
      });
    });

    var world = viewportWorld();
    fitCamera(world);
    buildGrid(world);
    ready = true;
    start();
  }, undefined, function () {
    console.warn('Animated background: SVG load failed');
  });
}

function start() {
  if (running || !ready) return;
  running = true;
  tick();
}

function tick() {
  if (!running) return;
  animId = requestAnimationFrame(tick);
  var t = clock.getElapsedTime();

  for (var i = 0; i < tiles.length; i++) {
    var tg = tiles[i];
    var ph = tg.userData.phase;
    var items = tg.userData.items;

    tg.position.y = tg.userData.baseY + Math.sin(t * 0.18 + ph) * DRIFT_Y;
    tg.position.x = tg.userData.baseX + Math.cos(t * 0.13 + ph * 0.7) * DRIFT_X;
    tg.rotation.z = Math.sin(t * 0.1 + ph * 1.1) * ROCK_MAX;

    var glow = 0.14 + 0.38 * Math.abs(Math.sin(t * 0.42 + ph * 1.9));
    var breathe = 0.1 + 0.05 * Math.sin(t * 0.75 + ph * 2.3);
    for (var j = 0; j < items.length; j += 2) {
      items[j].opacity = breathe;
      items[j + 1].opacity = glow;
    }
    if (i === 0 && window.__themeAnimated) window.__themeAnimated._lastGlow = glow;
  }

  renderer.render(scene, camera);
}

function stop() {
  running = false;
  if (animId) { cancelAnimationFrame(animId); animId = null; }
}

function setTheme(theme) {
  localStorage.setItem(STATE_KEY, theme);
  document.body.classList.toggle('theme-animated', theme === 'animated');

  var btns = document.querySelectorAll('.theme-btn');
  for (var i = 0; i < btns.length; i++) {
    btns[i].classList.toggle('active', btns[i].dataset.theme === theme);
  }

  if (theme === 'animated') {
    if (!renderer) init();
    else start();
  } else {
    stop();
  }
}

function getTheme() {
  return localStorage.getItem(STATE_KEY) || 'static';
}

function boot() {
  setTheme(getTheme());
  var btns = document.querySelectorAll('.theme-btn');
  for (var i = 0; i < btns.length; i++) {
    btns[i].addEventListener('click', function () {
      setTheme(this.dataset.theme);
    });
  }
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', boot);
} else {
  boot();
}