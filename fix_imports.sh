#!/bin/bash
for file in \
  "app/src/androidTest/kotlin/com/deproof/ui/viewmodel/ProofSubmissionViewModelTest.kt" \
  "app/src/androidTest/kotlin/com/deproof/ui/integration/ProofSubmissionFlowTest.kt" \
  "app/src/androidTest/kotlin/com/deproof/ui/screen/ProofSubmissionScreenTest.kt"
do
  echo "Fixing $file..."
  # Find line number of first import
  firstimport=$(grep -n "^import " "$file" | head -1 | cut -d: -f1)
  if [ -z "$firstimport" ]; then
    firstimport=$(($(grep -n "^package " "$file" | cut -d: -f1) + 1))
  fi
  # Create temp file with fixed imports at right position
  head -n $((firstimport - 1)) "$file" > "$file.tmp"
  echo "import org.junit.Test" >> "$file.tmp"
  echo "import kotlin.test.assertEquals" >> "$file.tmp"
  echo "import kotlin.test.assertNotNull" >> "$file.tmp"
  echo "import kotlin.test.assertNull" >> "$file.tmp"
  echo "import kotlin.test.assertTrue" >> "$file.tmp"
  echo "import kotlin.test.assertFalse" >> "$file.tmp"
  tail -n +$firstimport "$file" | grep -v "^import kotlin.test\|^import org.junit.Test" >> "$file.tmp"
  mv "$file.tmp" "$file"
done
