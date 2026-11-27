# Model migration source models

During each model-migration stage, the source model is read-only. The first
stage reads the loaded XMI model; later stages read backups of the preceding
stage's target. Migration callbacks may read source objects and write the
evolving target model. They may also change the evolving Ecore metamodel.

Changing a source object's attributes, references, contained objects, lists,
or resource roots fails the migration. This check runs in the shared runtime
for generated migrations and live previews. A detected write remains a failure
even if callback code catches the immediate exception. EMF reports the change
after it happens, so the guard does not undo an in-memory write; failed output
is not published or saved as a successful migration. Source XMI files are not
saved by the guard.

Live previews report the failure as an editor validation error at the
offending Edelta expression when its source location is reliable, otherwise
at the enclosing migration or user-owned refactoring call. Ordinary generated
migrations fail with an exception identifying the source object and feature.

The guard installs one adapter through each selected source resource's
containment tree when a stage starts and removes it when the source changes or
migration ends. It does not walk non-containment links. This adds traversal
work proportional to the source model size at stage boundaries and constant
work for delivered source notifications.
