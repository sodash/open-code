
def compile_names(in_filename,out_filename):
    in_file = open(in_filename)
    cells = [[item.strip() for item in row.split(',')] for row in in_file.readlines()]
    # to flat list ....
    cells_flat = [cell for row in cells for cell in row]
    names = [name.lower() for name in cells_flat if name.isalpha()]
    # uniquify and alphabetise
    names = sorted(list(set(names)))
    out_file = open(out_filename,'w')
    out_file.write('\n'.join(names)+'\n')
    return names

if __name__=='__main__':
    girls = compile_names('uk_historic_girls.csv','namelist_en-gb_f.txt')
    boys = compile_names('uk_historic_boys.csv','namelist_en-gb_m.txt')
    print set(boys).intersection(set(girls))
