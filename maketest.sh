#!/bin/bash
[[ -e colors.html ]] && rm colors.html
for i in $(cat colors.csv); do echo '<div class="foo" style="background-color:rgb('$i')"></div>' >> colors.html; done
cat testheader.html colors.html > fulltest.html
echo '</body></html>' >> fulltest.html
