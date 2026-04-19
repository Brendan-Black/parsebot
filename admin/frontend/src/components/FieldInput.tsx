import { useState } from 'preact/hooks';
import { Field, FileChooserMode, api } from '../api';
import { FieldType } from '../consts';

interface Props {
  field: Field;
  value: string;
  onChange: (v: string) => void;
}

export function FieldInput({ field, value, onChange }: Props) {
  const onInput = (e: Event) => onChange((e.target as HTMLInputElement).value);

  let control;
  switch (field.type) {
    case FieldType.PASSWORD:
      control = <input type="password" value={value} onInput={onInput} />;
      break;
    case FieldType.BOOLEAN:
      control = (
        <select value={value} onInput={onInput}>
          <option value="true">Yes</option>
          <option value="false">No</option>
        </select>
      );
      break;
    case FieldType.TIME:
      control = <input type="time" value={value} onInput={onInput} />;
      break;
    case FieldType.LIST: {
      const onListInput = (e: Event) => {
        const raw = (e.target as HTMLTextAreaElement).value;
        const items = raw
          .split(/\r?\n/)
          .map((s) => s.trim())
          .filter(Boolean);
        onChange(items.join(','));
      };
      const display = value ? value.split(',').join('\n') : '';
      return (
        <div class="field multiline">
          <label>{field.label}</label>
          <textarea rows={3} value={display} onInput={onListInput} />
        </div>
      );
    }
    case FieldType.FILE:
      control = <PathPicker mode="file" field={field} value={value} onChange={onChange} onInput={onInput} />;
      break;
    case FieldType.FOLDER:
      control = <PathPicker mode="folder" field={field} value={value} onChange={onChange} onInput={onInput} />;
      break;
    case FieldType.TEXT:
    default:
      control = <input type="text" value={value} onInput={onInput} />;
  }

  return (
    <div class="field">
      <label>{field.label}</label>
      {control}
    </div>
  );
}

interface PathPickerProps {
  mode: FileChooserMode;
  field: Field;
  value: string;
  onChange: (v: string) => void;
  onInput: (e: Event) => void;
}

function PathPicker({ mode, field, value, onChange, onInput }: PathPickerProps) {
  const [busy, setBusy] = useState(false);

  const onBrowse = async () => {
    setBusy(true);
    try {
      const r = await api.fileChooser(mode, value, field.label);
      if (r.path) onChange(r.path);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div class="path-picker">
      <input type="text" value={value} onInput={onInput} />
      <button type="button" onClick={onBrowse} disabled={busy}>
        {busy ? '…' : 'Browse…'}
      </button>
    </div>
  );
}
