package black.parsebot.ps;

import java.util.List;

public final class WinFormsScript {

  private WinFormsScript() {}

  public static void form(StringBuilder ps, String varName, String title,
              int width, int height,
              boolean fixedDialog, boolean topMost) {
    String v = "$" + varName;
    ps.append(v).append(" = New-Object System.Windows.Forms.Form\n");
    ps.append(v).append(".Text = '").append(PowerShellRunner.escapeSingleQuote(title)).append("'\n");
    ps.append(v).append(".StartPosition = 'CenterScreen'\n");
    ps.append(v).append(".ClientSize = New-Object System.Drawing.Size(").append(width).append(",").append(height).append(")\n");
    if (fixedDialog) {
      ps.append(v).append(".FormBorderStyle = 'FixedDialog'\n");
      ps.append(v).append(".MaximizeBox = $false\n");
    }
    if (topMost) {
      ps.append(v).append(".TopMost = $true\n");
    }
  }

  public static void label(StringBuilder ps, String varName, String parentVar,
              int x, int y, int w, int h, String textLiteral) {
    String v = "$" + varName;
    ps.append(v).append(" = New-Object System.Windows.Forms.Label\n");
    ps.append(v).append(".Location = New-Object System.Drawing.Point(").append(x).append(",").append(y).append(")\n");
    ps.append(v).append(".Size = New-Object System.Drawing.Size(").append(w).append(",").append(h).append(")\n");
    ps.append(v).append(".Text = '").append(PowerShellRunner.escapeSingleQuote(textLiteral)).append("'\n");
    ps.append("$").append(parentVar).append(".Controls.Add(").append(v).append(")\n");
  }

  public static void button(StringBuilder ps, String varName, String parentVar,
               int x, int y, int w, int h, String textLiteral,
               String dialogResult) {
    String v = "$" + varName;
    ps.append(v).append(" = New-Object System.Windows.Forms.Button\n");
    ps.append(v).append(".Location = New-Object System.Drawing.Point(").append(x).append(",").append(y).append(")\n");
    ps.append(v).append(".Size = New-Object System.Drawing.Size(").append(w).append(",").append(h).append(")\n");
    ps.append(v).append(".Text = '").append(PowerShellRunner.escapeSingleQuote(textLiteral)).append("'\n");
    if (dialogResult != null) {
      ps.append(v).append(".DialogResult = [System.Windows.Forms.DialogResult]::").append(dialogResult).append("\n");
    }
    ps.append("$").append(parentVar).append(".Controls.Add(").append(v).append(")\n");
  }

  public static void dataGridView(StringBuilder ps, String varName, String parentVar,
                  int x, int y, int w, int h,
                  List<String> columnNames, List<Integer> fillWeights) {
    if (columnNames.size() != fillWeights.size()) {
      throw new IllegalArgumentException("columnNames and fillWeights must have the same size");
    }
    String v = "$" + varName;
    ps.append(v).append(" = New-Object System.Windows.Forms.DataGridView\n");
    ps.append(v).append(".Location = New-Object System.Drawing.Point(").append(x).append(",").append(y).append(")\n");
    ps.append(v).append(".Size = New-Object System.Drawing.Size(").append(w).append(",").append(h).append(")\n");
    ps.append(v).append(".ReadOnly = $true\n");
    ps.append(v).append(".AllowUserToAddRows = $false\n");
    ps.append(v).append(".AllowUserToDeleteRows = $false\n");
    ps.append(v).append(".AutoSizeColumnsMode = 'Fill'\n");
    ps.append(v).append(".SelectionMode = 'FullRowSelect'\n");
    ps.append(v).append(".RowHeadersVisible = $false\n");
    for (String name : columnNames) {
      String escaped = PowerShellRunner.escapeSingleQuote(name);
      ps.append(v).append(".Columns.Add('").append(escaped).append("', '").append(escaped).append("') | Out-Null\n");
    }
    for (int i = 0; i < columnNames.size(); i++) {
      String escaped = PowerShellRunner.escapeSingleQuote(columnNames.get(i));
      ps.append(v).append(".Columns['").append(escaped).append("'].FillWeight = ").append(fillWeights.get(i)).append("\n");
    }
    ps.append("$").append(parentVar).append(".Controls.Add(").append(v).append(")\n");
  }

  public static void statusBar(StringBuilder ps, String varName, String parentVar,
                String textLiteral) {
    String v = "$" + varName;
    ps.append(v).append(" = New-Object System.Windows.Forms.StatusBar\n");
    ps.append(v).append(".Text = '").append(PowerShellRunner.escapeSingleQuote(textLiteral)).append("'\n");
    ps.append("$").append(parentVar).append(".Controls.Add(").append(v).append(")\n");
  }
}
