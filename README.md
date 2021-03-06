# cljs-threejs-playground

Playing with threejs and cljs [online demo](https://rafael.delboni.cc/cljs-threejs-playground/)

## Current Studies

### Loading a .GLTF File
- [tutorial](https://threejs.org/manual/#en/load-gltf)
- [source](https://threejs.org/manual/examples/resources/editor.html?url=/manual/examples/load-gltf.html)

### Voxel Geometry
- [tutorial](https://threejs.org/manual/#en/voxel-geometry)
- [source](https://threejs.org/manual/examples/resources/editor.html?url=/manual/examples/voxel-geometry-culled-faces-with-textures.html)

## Prerequisites
Things you need installed to use this repository

- [nodejs](https://nodejs.dev/download)
- [clojure](https://clojure.org/guides/getting_started)

### Install dependencies
```bash
npm install
```

## Commands

### Local build
Start shadow-cljs watching and serving main in [`localhost:8000`](http://localhost:8000)
```bash
npm run watch
```

### Deploy
Build the release package to production deploy
```bash
npm run release
```

# License
This is free and unencumbered software released into the public domain.  
For more information, please refer to <http://unlicense.org>
