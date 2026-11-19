# Référencées par nom qualifié dans le manifeste et les layouts XML :
# le système et l'inflater les instancient par reflection, le nom doit survivre à R8.
-keep class fr.famille.modeenfant.GuardService { *; }
-keep class fr.famille.modeenfant.ParentActivity { *; }
-keep class fr.famille.modeenfant.KidHomeActivity { *; }
-keep class fr.famille.modeenfant.AppListActivity { *; }
-keep class fr.famille.modeenfant.TimerView { *; }
-keep class fr.famille.modeenfant.HoldRingView { *; }
