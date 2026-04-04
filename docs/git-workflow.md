# Git Workflow Guide — FRC Team 7737

This is a beginner-friendly guide for all team members. You don't need to be on software to follow this — anyone committing documents, drawings, or SOPs should know the basics.

---

## Core Concept: What is Git?

Git tracks **every change** ever made to files in this repo. Think of it like unlimited "undo" history plus the ability for multiple people to work simultaneously without overwriting each other.

GitHub is where this repo lives online. Git is the tool that syncs your local computer with GitHub.

---

## The Golden Rules

1. **Never push directly to `main`.**  `main` is the competition-ready branch. All changes go through a Pull Request.
2. **Always branch from `develop`**, not `main`.
3. **Commit small and often.** Small commits are easy to review and easy to revert.
4. **Write useful commit messages.** `Fix shooter` is bad. `Fix shooter speed from 3500 to 4200 RPM for high goal` is good.
5. **Pass the build before opening a PR.** Run `./gradlew build` and confirm it succeeds.

---

## Standard Workflow (Step by Step)

### 1. Get the latest code
```bash
git checkout develop
git pull origin develop
```

### 2. Create a new branch

Use this naming convention:
```
YYYY/feature/<short-description>    # new capability
YYYY/fix/<short-description>        # bug fix
YYYY/docs/<short-description>       # document/resource update
```

Examples:
```bash
git checkout -b 2026/feature/autonomous-two-piece
git checkout -b 2026/fix/intake-jam-detection
git checkout -b 2026/docs/electrical-wiring-diagram
```

### 3. Make your changes

Edit files, add documents, write code.

### 4. Stage and commit your changes
```bash
git add .                                  # stage all changed files
git status                                 # verify what's staged
git commit -m "Add two-piece autonomous routine for center start"
```

### 5. Push your branch to GitHub
```bash
git push origin 2026/feature/autonomous-two-piece
```

### 6. Open a Pull Request

Go to the repo on GitHub → you'll see a banner "Compare & pull request" → click it.

Fill out the PR template:
- Describe what changed and why
- Check off the testing checklist
- Request a reviewer (software lead, mentor)

### 7. Address review feedback

Reviewers may leave comments. Make the changes on your branch, commit, and push again — the PR updates automatically.

### 8. Merge

Once approved, the PR is merged into `develop`. Periodically, `develop` is merged into `main` after full competition testing.

---

## Useful Commands Quick Reference

| Command | What it does |
|---------|-------------|
| `git status` | See what files changed |
| `git log --oneline -10` | See the last 10 commits |
| `git diff` | See exact line changes not yet committed |
| `git stash` | Temporarily shelve uncommitted changes |
| `git stash pop` | Restore stashed changes |
| `git checkout -- <file>` | Discard changes to a single file |
| `git fetch origin` | Download latest from GitHub without merging |
| `git pull origin develop` | Download AND merge latest develop |

---

## Emergency: Revert to Last Known-Good Code

If bad code was deployed and you need to roll back fast:

```bash
# Find the last good commit
git log --oneline -10

# Check out just the robot source from that commit (non-destructive)
git checkout <commit-hash> -- 2026/robot/src/

# Build and deploy
cd 2026/robot
./gradlew deploy
```

---

## Competition Day: Read-Only Policy

During a competition, **only the Drive Coach or Software Lead may approve code changes**. The process is:

1. Create a fix branch on the laptop
2. Test on the robot in the pit
3. Commit the fix, push, open PR
4. Mentor approves and merges
5. Deploy from `main`

No freestyle edits to `main` under match pressure.
