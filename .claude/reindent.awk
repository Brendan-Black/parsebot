{
  line = $0
  col = 0
  i = 1
  while (i <= length(line)) {
    c = substr(line, i, 1)
    if (c == "\t") {
      col += 4 - (col % 4)
    } else if (c == " ") {
      col += 1
    } else {
      break
    }
    i++
  }
  rest = substr(line, i)
  new_col = int(col / 2)
  pad = ""
  for (j = 0; j < new_col; j++) pad = pad " "
  print pad rest
}
