#!/usr/bin/perl -w

open FREQS, "zcat bnc_all.al.gz |";

my $firstline = <FREQS>;
$firstline =~/ANY\s*(\d+)/;
my $corpus_size = $1;

my %totals;

while (<FREQS>) {
	my @bits = split;
	$totals{$bits[1]} += $bits[3];
	# ugly hack
	if ($totals{$bits[1]} > $corpus_size) {
		$totals{$bits[1]} = $corpus_size;
	}
}

open OUTFILE, ">bnc_simple.al";
print OUTFILE "$corpus_size\n";
foreach my $word (sort keys(%totals)) {
	print OUTFILE "$word $totals{$word}\n";
}
unlink("bnc_simple.al.gz");
system("gzip bnc_simple.al");
