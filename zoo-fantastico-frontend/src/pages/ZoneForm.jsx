import React, { useEffect, useState } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { useZoo } from '../context/ZooContext';
import * as zoneService from '../api/zoneService';
import { Shield, Save, ArrowLeft } from 'lucide-react';

const EMPTY_FORM = { name: '', description: '', capacity: 1 };

export const ZoneForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = Boolean(id);
  const { zones, addZone, updateZone, showNotification } = useZoo();

  const [form, setForm] = useState(EMPTY_FORM);
  const [loadingZone, setLoadingZone] = useState(isEdit);
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState({});

  useEffect(() => {
    if (!isEdit) return;
    const numericId = Number(id);
    const existing = zones.find(z => z.id === numericId);

    if (existing) {
      setForm({ name: existing.name, description: existing.description || '', capacity: existing.capacity });
      setLoadingZone(false);
      return;
    }

    zoneService.getZoneById(numericId)
      .then(data => setForm({ name: data.name, description: data.description || '', capacity: data.capacity }))
      .catch(err => {
        showNotification(err?.response?.data?.message || 'Zona no encontrada en el registro.', 'error', err?.response?.status);
        navigate('/zones');
      })
      .finally(() => setLoadingZone(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, zones]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, [name]: value }));
    setErrors(prev => (prev[name] ? { ...prev, [name]: undefined } : prev));
  };

  const validate = () => {
    const nextErrors = {};
    if (!form.name.trim()) {
      nextErrors.name = 'El nombre es obligatorio.';
    }
    if (form.capacity === '' || Number.isNaN(Number(form.capacity)) || Number(form.capacity) < 1) {
      nextErrors.capacity = 'La capacidad debe ser un número entero mayor o igual a 1.';
    }
    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;
    setSubmitting(true);

    const payload = { name: form.name, description: form.description, capacity: form.capacity };
    const result = isEdit ? await updateZone(Number(id), payload) : await addZone(payload);

    setSubmitting(false);
    if (result) {
      navigate('/zones');
    }
  };

  if (loadingZone) {
    return (
      <div className="container" style={{ padding: '3rem 0', textAlign: 'center' }}>
        <p>Consultando el hábitat...</p>
      </div>
    );
  }

  return (
    <div className="container" style={{ paddingTop: '2rem', paddingBottom: '4rem', maxWidth: '640px' }}>
      <Link to="/zones" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', marginBottom: '1.5rem' }}>
        <ArrowLeft size={16} /> Volver a las zonas
      </Link>

      <h1 style={{ fontSize: '1.8rem', marginBottom: '0.3rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
        <Shield size={24} color="#c9a13b" /> {isEdit ? 'Editar Zona / Hábitat' : 'Crear Nueva Zona'}
      </h1>
      <p style={{ color: '#b8ac97', marginBottom: '2rem' }}>
        {isEdit ? 'Actualiza los datos del hábitat protegido.' : 'Define un nuevo hábitat dimensional para las criaturas.'}
      </p>

      <form onSubmit={handleSubmit} className="magic-card" noValidate>
        <div className="form-group">
          <label className="form-label">Nombre</label>
          <input className="form-input" name="name" value={form.name} onChange={handleChange} required />
          {errors.name && (
            <p style={{ color: '#e74c3c', fontSize: '0.8rem', marginTop: '0.35rem' }}>{errors.name}</p>
          )}
        </div>

        <div className="form-group">
          <label className="form-label">Descripción</label>
          <textarea className="form-textarea" name="description" value={form.description} onChange={handleChange} rows={3} />
        </div>

        <div className="form-group">
          <label className="form-label">Capacidad</label>
          <input className="form-input" type="number" min="1" step="1" name="capacity" value={form.capacity} onChange={handleChange} required />
          {errors.capacity && (
            <p style={{ color: '#e74c3c', fontSize: '0.8rem', marginTop: '0.35rem' }}>{errors.capacity}</p>
          )}
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
          <Link to="/zones" className="btn-magical">Cancelar</Link>
          <button type="submit" className="btn-magical btn-magical-primary" disabled={submitting}>
            <Save size={16} /> {submitting ? 'Guardando...' : 'Guardar'}
          </button>
        </div>
      </form>
    </div>
  );
};
