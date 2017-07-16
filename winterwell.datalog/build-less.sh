# Build our less files

lessc web/style/main.less web/style/main.css
lessc web/style/print.less web/style/print.css

# lessc "$file" "${file%.less}.css"

# for file in web/style/*.less; do
# 	if [ -e "$file" ]; then
# 		lessc "$file" "${file%.less}.css"
# 	else
# 		echo "no less files found"
# 	exit 2
# 	fi
# done
