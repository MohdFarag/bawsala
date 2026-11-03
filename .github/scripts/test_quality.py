import copy
import unittest

from quality import select, validate


class QualityTests(unittest.TestCase):
    def test_project_selection(self):
        self.assertEqual(select(["README.md"]), dict.fromkeys(("android", "server", "web", "devices"), False))
        self.assertEqual(select(["server/src/app.ts"]), {"android": False, "server": True, "web": False, "devices": True})
        self.assertTrue(all(select([".github/scripts/check.py"]).values()))
        self.assertTrue(all(select([], full=True).values()))
        self.assertTrue(select(["android/old.kt", "docs/moved.kt"])["android"])
        self.assertTrue(select([".prettierrc.json"])["web"])

    def test_invalid_selection_never_authorizes_a_skip(self):
        with self.assertRaises(ValueError):
            validate({"changes": {"result": "success", "outputs": {"server": "unknown"}}, "server": {"result": "skipped"}})

    def test_gate_rejects_failures_and_unexpected_skips(self):
        needs = {
            "changes": {"result": "success", "outputs": {"server": "true", "android": "false"}},
            "server": {"result": "success"},
            "android": {"result": "skipped"},
            "security": {"result": "success"},
        }
        validate(needs)
        for job in ("server", "security", "changes"):
            for outcome in ("failure", "cancelled", "skipped"):
                broken = copy.deepcopy(needs)
                broken[job]["result"] = outcome
                with self.subTest(job=job, outcome=outcome), self.assertRaises(ValueError):
                    validate(broken)


if __name__ == "__main__":
    unittest.main()
