# Reporting corrections retained

An initial qualification run passed core/node/verifier checks but the coverage/report scripts raised `KeyError: 'functions'`: the supplied registry uses separate `base` and `ecosystem` arrays. The scripts now operate on those exact arrays without renaming IDs or duplicating contracts. Exact 62 FN + 24 EF counts are checked.

The initial site link check also failed because qualification-report.md had not yet been generated. Qualification now creates a preliminary report before site-link checks and regenerates the final report after recording those checks. Corrected registry/site runs passed. Android/Gradle and prover infrastructure failures remain nonzero and are not suppressed. New runs preserve previous/latest log copies and append command events under runs/ and qualification-events.jsonl.
