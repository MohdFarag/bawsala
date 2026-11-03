import unittest

from release_policy import promotions


class ReleasePolicyTests(unittest.TestCase):
    def test_new_release_advances_both_aliases(self):
        self.assertEqual(promotions("v0.11.0", ["v0.9.0", "v0.10.0"]), {"latest": True, "series": True})

    def test_old_rerun_cannot_downgrade_latest_or_series(self):
        self.assertEqual(promotions("v0.10.0", ["v0.10.1", "v0.11.0"]), {"latest": False, "series": False})
        self.assertEqual(promotions("v0.10.1", ["v0.10.0", "v0.11.0"]), {"latest": False, "series": True})

    def test_versions_are_numeric_and_stable(self):
        self.assertFalse(promotions("v0.9.0", ["v0.10.0", "v0.11.0-beta"])["latest"])
        with self.assertRaises(ValueError):
            promotions("v0.10.1-beta", [])
