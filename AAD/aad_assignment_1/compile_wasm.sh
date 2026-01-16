#!/bin/bash
set -e

 SERVE=1; OPEN=1; PORT=8000; DO_BUILD=1

 echo "DETI Coin WASM build (scalar + SIMD)"
 echo "Fixed mode: Build + Serve + Open (PORT=$PORT)"
 echo "Mode: DO_BUILD=$DO_BUILD SERVE=$SERVE OPEN=$OPEN PORT=$PORT"

if [[ $DO_BUILD -eq 1 ]]; then
  if ! command -v emcc &>/dev/null; then
    if [[ -f "$HOME/emsdk/emsdk_env.sh" ]]; then
      source "$HOME/emsdk/emsdk_env.sh"
    fi
  fi
  if ! command -v emcc &>/dev/null; then
    echo "Emscripten not found (install emsdk or source env)."; exit 1
  fi
  echo "emcc: $(emcc --version | head -n 1)"
fi

mkdir -p WebAssembly

if [[ $DO_BUILD -eq 1 ]]; then
  mkdir -p WebAssembly
  SCALAR_EXPORTS='["_search_coins","_set_difficulty","_get_attempts","_get_coins_found","_get_first_coin_nonce","_get_first_coin_ptr","_get_first_coin_length","_get_coins_buffer_ptr","_get_coins_buffer_length","_get_random_nonce","_malloc","_free"]'
  SIMD_EXPORTS='["_search_coins_simd","_set_difficulty_simd","_get_attempts_simd","_get_coins_found_simd","_get_first_coin_nonce_simd","_get_first_coin_ptr_simd","_get_first_coin_length_simd","_get_coins_buffer_ptr_simd","_get_coins_buffer_length_simd","_malloc","_free"]'
  emcc -O3 wasm_search.c -o WebAssembly/wasm_search_scalar.js \
    -s EXPORTED_FUNCTIONS="$SCALAR_EXPORTS" \
    -s EXPORTED_RUNTIME_METHODS='["ccall","cwrap","HEAPU8"]' -s ALLOW_MEMORY_GROWTH=1 -s FORCE_FILESYSTEM=1 \
    -s MODULARIZE=1 -s EXPORT_NAME='WasmSearchScalar'
  emcc -O3 -msimd128 wasm_simd_search.c -o WebAssembly/wasm_search_simd.js \
    -s EXPORTED_FUNCTIONS="$SIMD_EXPORTS" \
    -s EXPORTED_RUNTIME_METHODS='["ccall","cwrap","HEAPU8"]' -s ALLOW_MEMORY_GROWTH=1 -s FORCE_FILESYSTEM=1 \
    -s MODULARIZE=1 -s EXPORT_NAME='WasmSearchSIMD'

    for f in WebAssembly/wasm_search_scalar.js WebAssembly/wasm_search_simd.js; do
      if command -v npx >/dev/null 2>&1; then
        npx --yes prettier@latest --loglevel warn --write "$f" >/dev/null 2>&1 || true
      elif python3 -m jsbeautifier --version >/dev/null 2>&1; then
        python3 -m jsbeautifier -r "$f" >/dev/null 2>&1 || true
      fi
    done
else
  echo "Skip build (--no-build)"
fi

ls -1 WebAssembly | grep wasm_search_ >/dev/null 2>&1 || echo "(No wasm_search_* artifacts found)"
if [[ $SERVE -eq 1 ]]; then
  echo "Serving from WebAssembly/ directory"
  echo "URL: http://localhost:$PORT/benchmark.html"
else
  echo "Run with --serve to start a server (URL then: http://localhost:$PORT/benchmark.html)"
fi

if [[ $SERVE -eq 1 ]]; then
  PY_CMD="python3"; command -v python3 >/dev/null 2>&1 || PY_CMD="python"
  ( cd WebAssembly && $PY_CMD -m http.server "$PORT" ) & SERVER_PID=$!
  sleep 1
  if [[ $OPEN -eq 1 ]] && command -v xdg-open >/dev/null 2>&1; then
    xdg-open "http://localhost:$PORT/benchmark.html" >/dev/null 2>&1 || true
  fi
  wait $SERVER_PID
fi