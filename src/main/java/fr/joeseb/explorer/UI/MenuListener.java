package fr.joeseb.explorer;

public interface MenuListener {
  void onElementSelected(ExplorerElement element);

  void onElementEdit(ExplorerElement element);

  void onBackSelected();
}
