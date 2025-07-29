OUT="out.txt"
echo "= Общие настройки" > $OUT
cat base.txt >> $OUT
echo "= MainActivity" >> $OUT
cat main.txt >> $OUT
echo "= DownloadActivity" >> $OUT
cat download.txt >> $OUT
tree .. >> $OUT
