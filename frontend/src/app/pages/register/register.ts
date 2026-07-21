import { Component, inject, signal } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { UserService } from '@app/services/user-service';

const MAX_BATCH_SIZE = 8;

type EntryForm = FormGroup<{
  name: FormControl<string>;
  cpf: FormControl<string>;
  picture: FormControl<File | null>;
}>;

function newEntryForm(): EntryForm {
  return new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    cpf: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    picture: new FormControl<File | null>(null, { validators: [Validators.required] }),
  });
}

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {
  private readonly userService = inject(UserService);
  private readonly router = inject(Router);

  protected readonly maxBatchSize = MAX_BATCH_SIZE;
  protected readonly entries = new FormArray<EntryForm>([newEntryForm()]);
  protected readonly form = new FormGroup({ entries: this.entries });

  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected onPictureSelected(event: Event, index: number): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.entries.at(index).controls.picture.setValue(file);
  }

  protected addEntry(): void {
    if (this.entries.length < MAX_BATCH_SIZE) {
      this.entries.push(newEntryForm());
    }
  }

  protected removeEntry(index: number): void {
    this.entries.removeAt(index);
  }

  protected onSubmit(): void {
    if (this.entries.invalid) {
      this.entries.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    const batchEntries = this.entries.controls.map((entry) => {
      const { name, cpf, picture } = entry.getRawValue();
      return { name, cpf, picture: picture! };
    });

    this.userService.createUsersBatch(batchEntries).subscribe({
      next: () => this.router.navigate(['/list']),
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(
          typeof err.error === 'string' ? err.error : 'Failed to register users. Please try again.',
        );
        this.loading.set(false);
      },
    });
  }
}
