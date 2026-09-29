import React, { useEffect, useState } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { useZoo } from '../context/ZooContext';
import * as zoneService from '../api/zoneService';
import * as creatureService from '../api/creatureService';
import { Sparkles, Save, ArrowLeft } from 'lucide-react';

const HEALTH_OPTIONS = ['healthy', 'stable', 'recovering', 'critical'];

const EMPTY_FORM = {
  name: '',
  species: '',
  size: '',
  dangerLevel: 1,
  healthStatus: 'healthy',
  zoneId: ''
};

export const CreatureForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = Boolean(id);
  const { creatures, addCreature, updateCreature, showNotification } = useZoo();

  const [form, setForm] = useState(EMPTY_FORM);
  const [zoneOptions, setZoneOptions] = useState([]);
  const [loadingZones, setLoadingZones] = useState(true);
  const [loadingCreature, setLoadingCreature] = useState(isEdit);
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState({});

  useEffect(() => {
    let active = true;
    zoneService.getAllZones()
      .then(data => { if (active) setZoneOptions(data); })
      .catch(() => {
        showNotification('No se pudieron cargar las zonas disponibles desde el backend.', 'error');
      })
      .finally(() => { if (active) setLoadingZones(false); });
    return () => { active = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!isEdit) return;
    const numericId = Number(id);
    const existing = creatures.find(c => c.id === numericId);

    if (existing) {
      setForm({
        name: existing.name,
        species: existing.species,
        size: existing.size,
        dangerLevel: existing.dangerLevel,
        healthStatus: existing.healthStatus,
        zoneId: existing.zoneId ?? ''
      });
      setLoadingCreature(false);
      return;
    }

    creatureService.getCreatureById(numericId)
      .then(data => setForm({
        name: data.name,
        species: data.species,
        size: data.size,
        dangerLevel: data.dangerLevel,
        healthStatus: data.healthStatus,
        zoneId: data.zoneId ?? ''
      }))
      .catch(err => {
        showNotification(err?.response?.data?.message || 'Criatura no encontrada en el registro.', 'error', err?.response?.status);
        navigate('/creatures');
      })
      .finally(() => setLoadingCreature(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, creatures]);

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
    if (!form.species.trim()) {
      nextErrors.species = 'La especie es obligatoria.';
    }
    if (form.zoneId === '' || form.zoneId == null) {
      nextErrors.zoneId = 'Debes seleccionar una zona.';
    }
    if (form.size === '' || Number.isNaN(Number(form.size)) || Number(form.size) < 0) {
      nextErrors.size = 'El tamaño (size) debe ser un número mayor o igual a 0.';
    }
    if (
      form.dangerLevel === '' ||
      Number.isNaN(Number(form.dangerLevel)) ||
      Number(form.dangerLevel) < 1 ||
      Number(form.dangerLevel) > 10
    ) {
      nextErrors.dangerLevel = 'El nivel de peligro debe estar entre 1 y 10.';
    }
    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;
    setSubmitting(true);

    const payload = {
      name: form.name,
      species: form.species,
      size: form.size,
      dangerLevel: form.dangerLevel,
      healthStatus: form.healthStatus,
      zoneId: form.zoneId
    };

    const result = isEdit
      ? await updateCreature(Number(id), payload)
      : await addCreature(payload);

    setSubmitting(false);
    if (result) {
      navigate('/creatures');
    }
  };

  if (loadingCreature) {
    return (
      <div className="container" style={{ padding: '3rem 0', textAlign: 'center' }}>
        <p>Consultando la ficha mágica...</p>
      </div>
    );
  }

  return (
    <div className="container" style={{ paddingTop: '2rem', paddingBottom: '4rem', maxWidth: '640px' }}>
      <Link to="/creatures" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', marginBottom: '1.5rem' }}>
        <ArrowLeft size={16} /> Volver al catálogo
      </Link>

      <h1 style={{ fontSize: '1.8rem', marginBottom: '0.3rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
        <Sparkles size={24} color="#c9a13b" /> {isEdit ? 'Editar Ficha de Criatura' : 'Registrar Nueva Criatura'}
      </h1>
      <p style={{ color: '#b8ac97', marginBottom: '2rem' }}>
        {isEdit ? 'Actualiza los datos del espécimen en el registro del Ministerio.' : 'Completa el pergamino para dar de alta un nuevo espécimen.'}
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
          <label className="form-label">Especie</label>
          <input className="form-input" name="species" value={form.species} onChange={handleChange} required />
          {errors.species && (
            <p style={{ color: '#e74c3c', fontSize: '0.8rem', marginTop: '0.35rem' }}>{errors.species}</p>
          )}
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
          <div className="form-group">
            <label className="form-label">Tamaño (m)</label>
            <input className="form-input" type="number" step="0.01" min="0" name="size" value={form.size} onChange={handleChange} required />
            {errors.size && (
              <p style={{ color: '#e74c3c', fontSize: '0.8rem', marginTop: '0.35rem' }}>{errors.size}</p>
            )}
          </div>

          <div className="form-group">
            <label className="form-label">Nivel de Peligro (1-10)</label>
            <input className="form-input" type="number" min="1" max="10" step="1" name="dangerLevel" value={form.dangerLevel} onChange={handleChange} required />
            {errors.dangerLevel && (
              <p style={{ color: '#e74c3c', fontSize: '0.8rem', marginTop: '0.35rem' }}>{errors.dangerLevel}</p>
            )}
          </div>
        </div>

        <div className="form-group">
          <label className="form-label">Estado de Salud</label>
          <select className="form-select" name="healthStatus" value={form.healthStatus} onChange={handleChange}>
            {HEALTH_OPTIONS.map(opt => (
              <option key={opt} value={opt}>{opt}</option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label className="form-label">Zona / Hábitat</label>
          <select className="form-select" name="zoneId" value={form.zoneId} onChange={handleChange} disabled={loadingZones} required>
            <option value="">{loadingZones ? 'Cargando zonas...' : 'Selecciona una zona'}</option>
            {zoneOptions.map(z => (
              <option key={z.id} value={z.id}>{z.name} ({z.creatureCount}/{z.capacity})</option>
            ))}
          </select>
          {errors.zoneId && (
            <p style={{ color: '#e74c3c', fontSize: '0.8rem', marginTop: '0.35rem' }}>{errors.zoneId}</p>
          )}
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
          <Link to="/creatures" className="btn-magical">Cancelar</Link>
          <button type="submit" className="btn-magical btn-magical-primary" disabled={submitting}>
            <Save size={16} /> {submitting ? 'Guardando...' : 'Guardar'}
          </button>
        </div>
      </form>
    </div>
  );
};
