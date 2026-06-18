import { useMemo, useState, type FormEvent } from "react";
import toast from "react-hot-toast";
import { useLocation, useNavigate } from "react-router";
import { z } from "zod";
import {
  BriefcaseBusiness,
  ClipboardList,
  Contact,
  FileText,
  NotebookText,
  Plus,
  UserPlus,
} from "lucide-react";
import { createContact } from "../services/contactService";
import { createCustomer, type CustomerType } from "../services/customerService";
import { createLead } from "../services/leadService";
import { createNote } from "../services/noteService";
import { createTask, type TaskPriority } from "../services/taskService";
import { createUser, type RoleName } from "../services/userService";
import { Modal, SelectField, TextAreaField, TextField } from "./ui";

type QuickCreateKind =
  | "customer"
  | "lead"
  | "contact"
  | "task"
  | "note"
  | "user";

type FormValues = Record<string, string>;

const quickActions = [
  { kind: "customer", label: "Customer", icon: BriefcaseBusiness },
  { kind: "lead", label: "Lead", icon: ClipboardList },
  { kind: "contact", label: "Contact", icon: Contact },
  { kind: "task", label: "Task", icon: NotebookText },
  { kind: "note", label: "Note", icon: FileText },
  { kind: "user", label: "User", icon: UserPlus },
] satisfies Array<{ kind: QuickCreateKind; label: string; icon: typeof Plus }>;

const optionalEmail = z
  .string()
  .trim()
  .refine((value) => value === "" || z.email().safeParse(value).success, {
    message: "Use a valid email or leave it empty",
  });

const optionalNumber = z
  .string()
  .trim()
  .refine((value) => value === "" || Number.isFinite(Number(value)), {
    message: "Use a valid number",
  });

const requiredNumber = z
  .string()
  .trim()
  .min(1, "Required")
  .refine((value) => Number.isInteger(Number(value)) && Number(value) > 0, {
    message: "Use a valid ID",
  });

const schemas = {
  customer: z.object({
    name: z.string().trim().min(2, "Customer name is required"),
    email: optionalEmail,
    phone: z.string().trim(),
    companyName: z.string().trim(),
    customerType: z.enum(["INDIVIDUAL", "COMPANY"]),
  }),
  lead: z.object({
    fullName: z.string().trim().min(2, "Lead name is required"),
    email: optionalEmail,
    phone: z.string().trim(),
    companyName: z.string().trim(),
    source: z.string().trim(),
    estimatedValue: optionalNumber,
    assignedToUserId: optionalNumber,
  }),
  contact: z.object({
    customerId: requiredNumber,
    fullName: z.string().trim().min(2, "Contact name is required"),
    email: optionalEmail,
    phone: z.string().trim(),
    position: z.string().trim(),
  }),
  task: z.object({
    title: z.string().trim().min(2, "Task title is required"),
    description: z.string().trim(),
    priority: z.enum(["LOW", "MEDIUM", "HIGH", "URGENT"]),
    dueDate: z.string().trim(),
    assignedToUserId: optionalNumber,
    customerId: optionalNumber,
    leadId: optionalNumber,
  }),
  note: z
    .object({
      content: z.string().trim().min(2, "Note content is required"),
      customerId: optionalNumber,
      leadId: optionalNumber,
    })
    .refine((value) => value.customerId !== "" || value.leadId !== "", {
      message: "Add a customer ID or lead ID",
      path: ["customerId"],
    }),
  user: z.object({
    fullName: z.string().trim().min(2, "Full name is required"),
    email: z.email("Valid email is required"),
    password: z.string().min(8, "Use at least 8 characters"),
    role: z.enum(["ADMIN", "MANAGER", "SALES_REP", "SUPPORT_STAFF"]),
  }),
} satisfies Record<QuickCreateKind, z.ZodType>;

const initialValues: Record<QuickCreateKind, FormValues> = {
  customer: {
    name: "",
    email: "",
    phone: "",
    companyName: "",
    customerType: "COMPANY",
  },
  lead: {
    fullName: "",
    email: "",
    phone: "",
    companyName: "",
    source: "Manual",
    estimatedValue: "",
    assignedToUserId: "",
  },
  contact: {
    customerId: "",
    fullName: "",
    email: "",
    phone: "",
    position: "",
  },
  task: {
    title: "",
    description: "",
    priority: "MEDIUM",
    dueDate: "",
    assignedToUserId: "",
    customerId: "",
    leadId: "",
  },
  note: {
    content: "",
    customerId: "",
    leadId: "",
  },
  user: {
    fullName: "",
    email: "",
    password: "",
    role: "SALES_REP",
  },
};

const successPath: Record<QuickCreateKind, string> = {
  customer: "/customers",
  lead: "/leads",
  contact: "/contacts",
  task: "/tasks",
  note: "/notes",
  user: "/users",
};

function emptyToNull(value: string) {
  const trimmed = value.trim();
  return trimmed === "" ? null : trimmed;
}

function numberOrNull(value: string) {
  const trimmed = value.trim();
  return trimmed === "" ? null : Number(trimmed);
}

function getApiMessage(error: unknown) {
  const apiError = error as {
    response?: { data?: { message?: string } };
    message?: string;
  };

  return apiError.response?.data?.message ?? apiError.message ?? "Request failed";
}

export function QuickCreateMenu() {
  const [menuOpen, setMenuOpen] = useState(false);
  const [activeKind, setActiveKind] = useState<QuickCreateKind | null>(null);
  const [formValues, setFormValues] = useState<FormValues>({});
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  const activeAction = useMemo(
    () => quickActions.find((action) => action.kind === activeKind),
    [activeKind],
  );

  function openForm(kind: QuickCreateKind) {
    setActiveKind(kind);
    setFormValues(initialValues[kind]);
    setErrors({});
    setMenuOpen(false);
  }

  function closeForm(force = false) {
    if (saving && !force) {
      return;
    }

    setActiveKind(null);
    setFormValues({});
    setErrors({});
  }

  function updateField(name: string, value: string) {
    setFormValues((current) => ({ ...current, [name]: value }));
    setErrors((current) => {
      const next = { ...current };
      delete next[name];
      return next;
    });
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!activeKind) {
      return;
    }

    const result = schemas[activeKind].safeParse(formValues);

    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      for (const issue of result.error.issues) {
        const field = String(issue.path[0] ?? "form");
        fieldErrors[field] = issue.message;
      }
      setErrors(fieldErrors);
      return;
    }

    setSaving(true);

    try {
      if (activeKind === "customer") {
        await createCustomer({
          name: formValues.name.trim(),
          email: emptyToNull(formValues.email),
          phone: emptyToNull(formValues.phone),
          companyName: emptyToNull(formValues.companyName),
          customerType: formValues.customerType as CustomerType,
        });
      }

      if (activeKind === "lead") {
        await createLead({
          fullName: formValues.fullName.trim(),
          email: emptyToNull(formValues.email),
          phone: emptyToNull(formValues.phone),
          companyName: emptyToNull(formValues.companyName),
          source: emptyToNull(formValues.source),
          estimatedValue: numberOrNull(formValues.estimatedValue),
          assignedToUserId: numberOrNull(formValues.assignedToUserId),
        });
      }

      if (activeKind === "contact") {
        await createContact({
          customerId: Number(formValues.customerId),
          fullName: formValues.fullName.trim(),
          email: emptyToNull(formValues.email),
          phone: emptyToNull(formValues.phone),
          position: emptyToNull(formValues.position),
        });
      }

      if (activeKind === "task") {
        await createTask({
          title: formValues.title.trim(),
          description: emptyToNull(formValues.description),
          priority: formValues.priority as TaskPriority,
          dueDate: emptyToNull(formValues.dueDate),
          assignedToUserId: numberOrNull(formValues.assignedToUserId),
          customerId: numberOrNull(formValues.customerId),
          leadId: numberOrNull(formValues.leadId),
        });
      }

      if (activeKind === "note") {
        await createNote({
          content: formValues.content.trim(),
          customerId: numberOrNull(formValues.customerId),
          leadId: numberOrNull(formValues.leadId),
        });
      }

      if (activeKind === "user") {
        await createUser({
          fullName: formValues.fullName.trim(),
          email: formValues.email.trim(),
          password: formValues.password,
          role: formValues.role as RoleName,
        });
      }

      toast.success(`${activeAction?.label ?? "Record"} created`);
      window.dispatchEvent(new Event("crm-data-changed"));
      if (location.pathname === successPath[activeKind]) {
        window.location.reload();
      } else {
        navigate(successPath[activeKind]);
      }
      closeForm(true);
    } catch (error) {
      toast.error(getApiMessage(error));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setMenuOpen((value) => !value)}
        className="inline-flex h-10 items-center gap-2 rounded-xl bg-[var(--crm-brand-gradient)] px-3 text-sm font-semibold text-white shadow-[0_14px_34px_rgba(6,74,92,0.22)] transition hover:-translate-y-0.5"
      >
        <Plus size={17} />
        <span className="hidden sm:inline">New</span>
      </button>

      {menuOpen && (
        <>
          <button
            type="button"
            className="fixed inset-0 z-40 cursor-default"
            aria-label="Close quick create menu"
            onClick={() => setMenuOpen(false)}
          />
          <div className="absolute right-0 top-12 z-50 w-64 overflow-hidden rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] p-2 text-[var(--crm-text)] shadow-[var(--crm-shadow-soft)]">
            <p className="px-3 pb-2 pt-1 text-xs font-semibold uppercase tracking-[0.16em] text-[var(--crm-text-muted)]">
              Quick create
            </p>
            {quickActions.map((action) => {
              const Icon = action.icon;

              return (
                <button
                  key={action.kind}
                  type="button"
                  onClick={() => openForm(action.kind)}
                  className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm font-medium transition hover:bg-cyan-400/10"
                >
                  <span className="grid h-9 w-9 place-items-center rounded-xl bg-cyan-400/10 text-cyan-200">
                    <Icon size={17} />
                  </span>
                  Add {action.label}
                </button>
              );
            })}
          </div>
        </>
      )}

      <Modal
        open={activeKind !== null}
        title={activeAction ? `Add ${activeAction.label}` : "Add record"}
        description="Create a real CRM record using the existing backend API."
        onClose={closeForm}
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          {activeKind === "customer" && (
            <>
              <TextField label="Customer name" value={formValues.name ?? ""} onChange={(event) => updateField("name", event.target.value)} error={errors.name} />
              <div className="grid gap-4 sm:grid-cols-2">
                <TextField label="Email" type="email" value={formValues.email ?? ""} onChange={(event) => updateField("email", event.target.value)} error={errors.email} />
                <TextField label="Phone" value={formValues.phone ?? ""} onChange={(event) => updateField("phone", event.target.value)} />
              </div>
              <TextField label="Company name" value={formValues.companyName ?? ""} onChange={(event) => updateField("companyName", event.target.value)} />
              <SelectField label="Customer type" value={formValues.customerType ?? "COMPANY"} onChange={(event) => updateField("customerType", event.target.value)} error={errors.customerType}>
                <option value="COMPANY">Company</option>
                <option value="INDIVIDUAL">Individual</option>
              </SelectField>
            </>
          )}

          {activeKind === "lead" && (
            <>
              <TextField label="Lead name" value={formValues.fullName ?? ""} onChange={(event) => updateField("fullName", event.target.value)} error={errors.fullName} />
              <div className="grid gap-4 sm:grid-cols-2">
                <TextField label="Email" type="email" value={formValues.email ?? ""} onChange={(event) => updateField("email", event.target.value)} error={errors.email} />
                <TextField label="Phone" value={formValues.phone ?? ""} onChange={(event) => updateField("phone", event.target.value)} />
              </div>
              <div className="grid gap-4 sm:grid-cols-2">
                <TextField label="Company" value={formValues.companyName ?? ""} onChange={(event) => updateField("companyName", event.target.value)} />
                <TextField label="Source" value={formValues.source ?? ""} onChange={(event) => updateField("source", event.target.value)} />
              </div>
              <div className="grid gap-4 sm:grid-cols-2">
                <TextField label="Estimated value" inputMode="decimal" value={formValues.estimatedValue ?? ""} onChange={(event) => updateField("estimatedValue", event.target.value)} error={errors.estimatedValue} />
                <TextField label="Assigned user ID" inputMode="numeric" value={formValues.assignedToUserId ?? ""} onChange={(event) => updateField("assignedToUserId", event.target.value)} error={errors.assignedToUserId} />
              </div>
            </>
          )}

          {activeKind === "contact" && (
            <>
              <TextField label="Customer ID" inputMode="numeric" value={formValues.customerId ?? ""} onChange={(event) => updateField("customerId", event.target.value)} error={errors.customerId} />
              <TextField label="Contact name" value={formValues.fullName ?? ""} onChange={(event) => updateField("fullName", event.target.value)} error={errors.fullName} />
              <div className="grid gap-4 sm:grid-cols-2">
                <TextField label="Email" type="email" value={formValues.email ?? ""} onChange={(event) => updateField("email", event.target.value)} error={errors.email} />
                <TextField label="Phone" value={formValues.phone ?? ""} onChange={(event) => updateField("phone", event.target.value)} />
              </div>
              <TextField label="Position" value={formValues.position ?? ""} onChange={(event) => updateField("position", event.target.value)} />
            </>
          )}

          {activeKind === "task" && (
            <>
              <TextField label="Task title" value={formValues.title ?? ""} onChange={(event) => updateField("title", event.target.value)} error={errors.title} />
              <TextAreaField label="Description" value={formValues.description ?? ""} onChange={(event) => updateField("description", event.target.value)} />
              <div className="grid gap-4 sm:grid-cols-2">
                <SelectField label="Priority" value={formValues.priority ?? "MEDIUM"} onChange={(event) => updateField("priority", event.target.value)} error={errors.priority}>
                  <option value="LOW">Low</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="HIGH">High</option>
                  <option value="URGENT">Urgent</option>
                </SelectField>
                <TextField label="Due date" type="datetime-local" value={formValues.dueDate ?? ""} onChange={(event) => updateField("dueDate", event.target.value)} />
              </div>
              <div className="grid gap-4 sm:grid-cols-3">
                <TextField label="Assigned user ID" inputMode="numeric" value={formValues.assignedToUserId ?? ""} onChange={(event) => updateField("assignedToUserId", event.target.value)} error={errors.assignedToUserId} />
                <TextField label="Customer ID" inputMode="numeric" value={formValues.customerId ?? ""} onChange={(event) => updateField("customerId", event.target.value)} error={errors.customerId} />
                <TextField label="Lead ID" inputMode="numeric" value={formValues.leadId ?? ""} onChange={(event) => updateField("leadId", event.target.value)} error={errors.leadId} />
              </div>
            </>
          )}

          {activeKind === "note" && (
            <>
              <TextAreaField label="Note" value={formValues.content ?? ""} onChange={(event) => updateField("content", event.target.value)} error={errors.content} />
              <div className="grid gap-4 sm:grid-cols-2">
                <TextField label="Customer ID" inputMode="numeric" value={formValues.customerId ?? ""} onChange={(event) => updateField("customerId", event.target.value)} error={errors.customerId} />
                <TextField label="Lead ID" inputMode="numeric" value={formValues.leadId ?? ""} onChange={(event) => updateField("leadId", event.target.value)} error={errors.leadId} />
              </div>
            </>
          )}

          {activeKind === "user" && (
            <>
              <TextField label="Full name" value={formValues.fullName ?? ""} onChange={(event) => updateField("fullName", event.target.value)} error={errors.fullName} />
              <TextField label="Email" type="email" value={formValues.email ?? ""} onChange={(event) => updateField("email", event.target.value)} error={errors.email} />
              <TextField label="Temporary password" type="password" value={formValues.password ?? ""} onChange={(event) => updateField("password", event.target.value)} error={errors.password} />
              <SelectField label="Role" value={formValues.role ?? "SALES_REP"} onChange={(event) => updateField("role", event.target.value)} error={errors.role}>
                <option value="ADMIN">Admin</option>
                <option value="MANAGER">Manager</option>
                <option value="SALES_REP">Sales rep</option>
                <option value="SUPPORT_STAFF">Support staff</option>
              </SelectField>
            </>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={() => closeForm()}
              className="h-10 rounded-xl border border-[var(--crm-border)] px-4 text-sm font-semibold text-[var(--crm-text-muted)] transition hover:bg-cyan-400/10 hover:text-[var(--crm-text)]"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving}
              className="h-10 rounded-xl bg-[var(--crm-brand-gradient)] px-4 text-sm font-semibold text-white shadow-[0_14px_34px_rgba(6,74,92,0.22)] transition hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:translate-y-0"
            >
              {saving ? "Creating..." : "Create"}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
