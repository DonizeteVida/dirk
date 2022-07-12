import com.google.devtools.ksp.symbol.KSClassDeclaration

fun KSClassDeclaration.getDeclaredClasses(): Sequence<KSClassDeclaration> =
    declarations.filterIsInstance<KSClassDeclaration>()