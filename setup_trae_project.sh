#!/bin/bash
set -euo pipefail

usage() {
  cat <<'EOF'
用法:
  setup_trae_project.sh [选项]

选项:
  -p, --project <path>  项目根目录 (默认: 当前目录)
  -g, --global  <path>  全局技能目录 (默认: $HOME/trae-global-skills)
  -n, --dry-run         仅打印将执行的操作，不做任何修改
  -h, --help            显示帮助
EOF
}

die() {
  echo "[ERROR] $*" >&2
  exit 1
}

info() {
  echo "[OK] $*"
}

warn() {
  echo "[WARN] $*" >&2
}

DRY_RUN=0
GLOBAL_SKILLS_DIR="${HOME}/trae-global-skills"
PROJECT_DIR="$(pwd)"

while [ $# -gt 0 ]; do
  case "$1" in
    -p|--project)
      [ $# -ge 2 ] || die "缺少参数: $1 <path>"
      PROJECT_DIR="$2"
      shift 2
      ;;
    -g|--global)
      [ $# -ge 2 ] || die "缺少参数: $1 <path>"
      GLOBAL_SKILLS_DIR="$2"
      shift 2
      ;;
    -n|--dry-run)
      DRY_RUN=1
      shift 1
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      die "未知参数: $1 (使用 -h 查看帮助)"
      ;;
  esac
done

[ -d "$GLOBAL_SKILLS_DIR" ] || die "全局技能目录不存在: $GLOBAL_SKILLS_DIR"
[ -d "$PROJECT_DIR" ] || die "项目目录不存在: $PROJECT_DIR"

PROJECT_DIR="$(cd "$PROJECT_DIR" && pwd -P)"
GLOBAL_SKILLS_DIR="$(cd "$GLOBAL_SKILLS_DIR" && pwd -P)"

TARGET_TRAE_DIR="${PROJECT_DIR}/.trae"
TARGET_SKILLS_PATH="${TARGET_TRAE_DIR}/skills"

if [ "$DRY_RUN" -eq 1 ]; then
  echo "[DRY-RUN] 将创建目录: $TARGET_TRAE_DIR"
else
  mkdir -p "$TARGET_TRAE_DIR"
fi

if [ -L "$TARGET_SKILLS_PATH" ]; then
  CURRENT_TARGET="$(readlink "$TARGET_SKILLS_PATH" || true)"
  if [ -n "$CURRENT_TARGET" ] && [ -d "$CURRENT_TARGET" ]; then
    CURRENT_TARGET="$(cd "$CURRENT_TARGET" && pwd -P)"
  elif [ -n "$CURRENT_TARGET" ] && [ -d "${TARGET_TRAE_DIR}/${CURRENT_TARGET}" ]; then
    CURRENT_TARGET="$(cd "${TARGET_TRAE_DIR}/${CURRENT_TARGET}" && pwd -P)"
  fi

  if [ "$CURRENT_TARGET" = "$GLOBAL_SKILLS_DIR" ]; then
    info "已存在正确的全局技能链接: $TARGET_SKILLS_PATH -> $GLOBAL_SKILLS_DIR"
    exit 0
  fi
fi

if [ -e "$TARGET_SKILLS_PATH" ] || [ -L "$TARGET_SKILLS_PATH" ]; then
  BACKUP_PATH="${TARGET_SKILLS_PATH}.bak.$(date +%Y%m%d_%H%M%S)"
  warn "检测到已存在的路径，将备份: $TARGET_SKILLS_PATH -> $BACKUP_PATH"
  if [ "$DRY_RUN" -eq 1 ]; then
    echo "[DRY-RUN] mv \"$TARGET_SKILLS_PATH\" \"$BACKUP_PATH\""
  else
    mv "$TARGET_SKILLS_PATH" "$BACKUP_PATH"
  fi
fi

if [ "$DRY_RUN" -eq 1 ]; then
  echo "[DRY-RUN] ln -s \"$GLOBAL_SKILLS_DIR\" \"$TARGET_SKILLS_PATH\""
else
  ln -s "$GLOBAL_SKILLS_DIR" "$TARGET_SKILLS_PATH"
  info "已在项目建立全局技能链接: $TARGET_SKILLS_PATH -> $GLOBAL_SKILLS_DIR"
fi
