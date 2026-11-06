import React, { useState } from 'react';
import { 
  Layers, 
  Settings, 
  Download, 
  Undo, 
  Redo, 
  GitBranch, 
  Plus, 
  Trash2, 
  Move, 
  RotateCw, 
  Maximize2, 
  Eye, 
  EyeOff, 
  Lock, 
  Unlock, 
  ExternalLink, 
  Sliders, 
  Copy, 
  Clipboard, 
  Image as ImageIcon, 
  Type as TypeIcon, 
  Square, 
  PenTool, 
  Sparkles,
  Check,
  Smartphone,
  ShieldCheck,
  Compass
} from 'lucide-react';

interface LayerItem {
  id: string;
  name: string;
  type: 'shape' | 'text' | 'media' | 'adjustment' | 'vector';
  x: number;
  y: number;
  scaleX: number;
  scaleY: number;
  isScaleLinked: boolean;
  rotation: number;
  color: string;
  visible: boolean;
  locked: boolean;
  opacity: number;
}

interface ActionTreeNode {
  id: string;
  title: string;
  timestamp: string;
  parentId: string | null;
  branch: string;
}

export default function App() {
  const [layers, setLayers] = useState<LayerItem[]>([
    {
      id: 'layer-1',
      name: 'Background Card',
      type: 'shape',
      x: 0,
      y: 0,
      scaleX: 1.5,
      scaleY: 1.5,
      isScaleLinked: true,
      rotation: 0,
      color: '#1A2338',
      visible: true,
      locked: false,
      opacity: 1
    },
    {
      id: 'layer-2',
      name: 'Vector Accent',
      type: 'vector',
      x: -40,
      y: -30,
      scaleX: 1.2,
      scaleY: 1.2,
      isScaleLinked: true,
      rotation: 15,
      color: '#00E5FF',
      visible: true,
      locked: false,
      opacity: 0.85
    },
    {
      id: 'layer-3',
      name: 'Hero Title',
      type: 'text',
      x: 0,
      y: 40,
      scaleX: 1,
      scaleY: 1,
      isScaleLinked: true,
      rotation: 0,
      color: '#FFFFFF',
      visible: true,
      locked: false,
      opacity: 1
    }
  ]);

  const [selectedLayerId, setSelectedLayerId] = useState<string>('layer-2');
  const [previewQuality, setPreviewQuality] = useState<'25%' | '50%' | '100%'>('100%');
  const [zoom, setZoom] = useState<number>(1);
  const [showGrid, setShowGrid] = useState<boolean>(true);
  const [activeTab, setActiveTab] = useState<'canvas' | 'action_tree' | 'settings' | 'build_status'>('canvas');

  // Action Tree nodes
  const [historyNodes, setHistoryNodes] = useState<ActionTreeNode[]>([
    { id: 'node-0', title: 'Create Canvas (1080x1920)', timestamp: '11:20:00', parentId: null, branch: 'main' },
    { id: 'node-1', title: 'Add Background Card', timestamp: '11:21:10', parentId: 'node-0', branch: 'main' },
    { id: 'node-2', title: 'Add Vector Accent', timestamp: '11:22:15', parentId: 'node-1', branch: 'main' },
    { id: 'node-3', title: 'Transform Vector (+15°)', timestamp: '11:23:40', parentId: 'node-2', branch: 'main' },
    { id: 'node-4', title: 'Branch Experiment: Glow Blend', timestamp: '11:25:00', parentId: 'node-2', branch: 'alt-glow' }
  ]);
  const [currentNodeId, setCurrentNodeId] = useState<string>('node-3');
  const [copiedLayerProps, setCopiedLayerProps] = useState<any | null>(null);

  const selectedLayer = layers.find(l => l.id === selectedLayerId);

  const updateSelectedLayer = (updates: Partial<LayerItem>) => {
    if (!selectedLayerId) return;
    setLayers(prev => prev.map(l => l.id === selectedLayerId ? { ...l, ...updates } : l));
  };

  const addLayer = (type: LayerItem['type']) => {
    const newId = `layer-${Date.now()}`;
    const newLayer: LayerItem = {
      id: newId,
      name: `${type.toUpperCase()} Layer ${layers.length + 1}`,
      type,
      x: (Math.random() - 0.5) * 80,
      y: (Math.random() - 0.5) * 80,
      scaleX: 1,
      scaleY: 1,
      isScaleLinked: true,
      rotation: 0,
      color: type === 'shape' ? '#00E5FF' : type === 'text' ? '#FFFFFF' : '#FFB300',
      visible: true,
      locked: false,
      opacity: 1
    };
    setLayers([newLayer, ...layers]);
    setSelectedLayerId(newId);

    // Record action tree
    const newNode: ActionTreeNode = {
      id: `node-${Date.now()}`,
      title: `Add ${newLayer.name}`,
      timestamp: new Date().toLocaleTimeString(),
      parentId: currentNodeId,
      branch: 'main'
    };
    setHistoryNodes([...historyNodes, newNode]);
    setCurrentNodeId(newNode.id);
  };

  return (
    <div className="flex flex-col h-screen w-screen bg-[#0C0E14] text-[#F0F4FC] font-sans overflow-hidden select-none">
      {/* Top Navigation Bar */}
      <header className="h-14 border-b border-[#1D2230] bg-[#141721] px-4 flex items-center justify-between shrink-0">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-[#00E5FF] to-[#2979FF] flex items-center justify-center font-black text-black text-sm shadow-[0_0_12px_rgba(0,229,255,0.4)]">
            SG
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-sm tracking-wider text-[#00E5FF]">SALTY GRAPHICS</span>
              <span className="text-[10px] bg-[#1D2230] text-[#98A4BC] px-1.5 py-0.5 rounded border border-[#2E364A]">Android Beta</span>
            </div>
            <p className="text-[11px] text-[#98A4BC]">motionssalt/salty-graphics</p>
          </div>
        </div>

        {/* Tab switchers */}
        <div className="flex items-center bg-[#0C0E14] p-1 rounded-lg border border-[#1D2230]">
          <button
            onClick={() => setActiveTab('canvas')}
            className={`px-3 py-1 text-xs rounded-md font-medium transition ${activeTab === 'canvas' ? 'bg-[#00E5FF] text-black shadow' : 'text-[#98A4BC] hover:text-white'}`}
          >
            Canvas Studio
          </button>
          <button
            onClick={() => setActiveTab('action_tree')}
            className={`px-3 py-1 text-xs rounded-md font-medium transition ${activeTab === 'action_tree' ? 'bg-[#00E5FF] text-black shadow' : 'text-[#98A4BC] hover:text-white'}`}
          >
            Action Tree ({historyNodes.length})
          </button>
          <button
            onClick={() => setActiveTab('build_status')}
            className={`px-3 py-1 text-xs rounded-md font-medium transition ${activeTab === 'build_status' ? 'bg-[#00E5FF] text-black shadow' : 'text-[#98A4BC] hover:text-white'}`}
          >
            Releases & APKs
          </button>
          <button
            onClick={() => setActiveTab('settings')}
            className={`px-3 py-1 text-xs rounded-md font-medium transition ${activeTab === 'settings' ? 'bg-[#00E5FF] text-black shadow' : 'text-[#98A4BC] hover:text-white'}`}
          >
            Settings & Info
          </button>
        </div>

        {/* Quick controls */}
        <div className="flex items-center gap-2">
          <button
            onClick={() => {
              const qualities: ('25%' | '50%' | '100%')[] = ['25%', '50%', '100%'];
              const next = qualities[(qualities.indexOf(previewQuality) + 1) % qualities.length];
              setPreviewQuality(next);
            }}
            className="px-2.5 py-1 text-xs rounded bg-[#1D2230] hover:bg-[#2E364A] text-[#00E5FF] border border-[#2E364A] font-mono"
            title="Preview Proxy Quality"
          >
            Proxy: {previewQuality}
          </button>
          <a
            href="https://github.com/motionssalt/salty-graphics"
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-1.5 px-3 py-1 text-xs bg-[#00E5FF]/10 text-[#00E5FF] hover:bg-[#00E5FF]/20 border border-[#00E5FF]/30 rounded transition font-medium"
          >
            <span>GitHub</span>
            <ExternalLink size={13} />
          </a>
        </div>
      </header>

      {/* Main Workspace Body */}
      <div className="flex-1 flex overflow-hidden">
        {activeTab === 'canvas' && (
          <>
            {/* Left Toolbar */}
            <aside className="w-16 border-r border-[#1D2230] bg-[#141721] flex flex-col items-center py-4 gap-4 shrink-0">
              <button
                onClick={() => addLayer('shape')}
                className="w-10 h-10 rounded-lg bg-[#1D2230] hover:bg-[#00E5FF] hover:text-black text-[#00E5FF] flex flex-col items-center justify-center transition group"
                title="Add Shape Layer"
              >
                <Square size={18} />
                <span className="text-[9px] mt-0.5">Shape</span>
              </button>
              <button
                onClick={() => addLayer('text')}
                className="w-10 h-10 rounded-lg bg-[#1D2230] hover:bg-[#00E5FF] hover:text-black text-[#00E5FF] flex flex-col items-center justify-center transition"
                title="Add Text Layer"
              >
                <TypeIcon size={18} />
                <span className="text-[9px] mt-0.5">Text</span>
              </button>
              <button
                onClick={() => addLayer('vector')}
                className="w-10 h-10 rounded-lg bg-[#1D2230] hover:bg-[#00E5FF] hover:text-black text-[#00E5FF] flex flex-col items-center justify-center transition"
                title="Add Vector Pen"
              >
                <PenTool size={18} />
                <span className="text-[9px] mt-0.5">Vector</span>
              </button>
              <button
                onClick={() => addLayer('media')}
                className="w-10 h-10 rounded-lg bg-[#1D2230] hover:bg-[#00E5FF] hover:text-black text-[#00E5FF] flex flex-col items-center justify-center transition"
                title="Add Media Layer"
              >
                <ImageIcon size={18} />
                <span className="text-[9px] mt-0.5">Media</span>
              </button>
              <button
                onClick={() => addLayer('adjustment')}
                className="w-10 h-10 rounded-lg bg-[#1D2230] hover:bg-[#00E5FF] hover:text-black text-[#00E5FF] flex flex-col items-center justify-center transition"
                title="Add Adjustment Layer"
              >
                <Sliders size={18} />
                <span className="text-[9px] mt-0.5">Adjust</span>
              </button>
            </aside>

            {/* Central Canvas Viewport */}
            <main className="flex-1 relative flex items-center justify-center bg-[#090B10] overflow-hidden">
              {/* Canvas viewport frame (representing Android 9:16 story ratio) */}
              <div 
                className="relative bg-black rounded shadow-2xl border border-[#2E364A] overflow-hidden transition-transform duration-100"
                style={{
                  width: `${360 * zoom}px`,
                  height: `${640 * zoom}px`,
                  backgroundImage: showGrid ? 'radial-gradient(#2E364A 1px, transparent 1px)' : 'none',
                  backgroundSize: `${20 * zoom}px ${20 * zoom}px`
                }}
              >
                {/* Render Layers */}
                {layers.filter(l => l.visible).map(layer => {
                  const isSelected = layer.id === selectedLayerId;
                  return (
                    <div
                      key={layer.id}
                      onClick={(e) => {
                        e.stopPropagation();
                        setSelectedLayerId(layer.id);
                      }}
                      className={`absolute cursor-pointer transition-shadow ${isSelected ? 'ring-2 ring-[#00E5FF] ring-offset-2 ring-offset-black' : ''}`}
                      style={{
                        left: `calc(50% + ${layer.x * zoom}px)`,
                        top: `calc(50% + ${layer.y * zoom}px)`,
                        transform: `translate(-50%, -50%) rotate(${layer.rotation}deg) scale(${layer.scaleX * zoom}, ${layer.scaleY * zoom})`,
                        opacity: layer.opacity
                      }}
                    >
                      {layer.type === 'shape' && (
                        <div 
                          className="w-32 h-32 rounded-2xl flex items-center justify-center border-2 border-white/20 shadow-lg"
                          style={{ backgroundColor: layer.color }}
                        >
                          <span className="text-[10px] text-white/80 font-bold uppercase">{layer.name}</span>
                        </div>
                      )}
                      {layer.type === 'text' && (
                        <div className="px-4 py-2 font-black text-2xl tracking-tight text-center drop-shadow" style={{ color: layer.color }}>
                          MOTIONSALT
                        </div>
                      )}
                      {layer.type === 'vector' && (
                        <div 
                          className="w-24 h-24 rounded-full border-4 border-dashed flex items-center justify-center"
                          style={{ borderColor: layer.color, backgroundColor: `${layer.color}22` }}
                        >
                          <PenTool size={20} color={layer.color} />
                        </div>
                      )}
                      {layer.type === 'media' && (
                        <div className="w-28 h-28 bg-[#1D2230] rounded-lg border border-[#2E364A] flex flex-col items-center justify-center p-2 text-center">
                          <ImageIcon size={24} className="text-[#98A4BC] mb-1" />
                          <span className="text-[10px] text-[#98A4BC]">{layer.name}</span>
                        </div>
                      )}
                      {layer.type === 'adjustment' && (
                        <div className="w-48 h-48 bg-[#00E5FF]/10 backdrop-blur-sm border border-[#00E5FF]/40 rounded-xl flex items-center justify-center">
                          <span className="text-[11px] text-[#00E5FF] font-semibold">Adjustment Filter</span>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>

              {/* Viewport Floating Zoom controls */}
              <div className="absolute bottom-4 left-4 bg-[#141721]/90 backdrop-blur border border-[#1D2230] rounded-lg p-1.5 flex items-center gap-2">
                <button 
                  onClick={() => setZoom(z => Math.max(0.4, z - 0.1))}
                  className="w-7 h-7 rounded bg-[#1D2230] hover:bg-[#2E364A] flex items-center justify-center text-xs"
                >
                  -
                </button>
                <span className="text-xs font-mono px-1">{(zoom * 100).toFixed(0)}%</span>
                <button 
                  onClick={() => setZoom(z => Math.min(2.5, z + 0.1))}
                  className="w-7 h-7 rounded bg-[#1D2230] hover:bg-[#2E364A] flex items-center justify-center text-xs"
                >
                  +
                </button>
                <div className="w-px h-4 bg-[#2E364A] mx-1" />
                <button
                  onClick={() => setShowGrid(!showGrid)}
                  className={`px-2 py-1 text-xs rounded ${showGrid ? 'bg-[#00E5FF]/20 text-[#00E5FF]' : 'text-[#98A4BC]'}`}
                >
                  Grid
                </button>
                <button
                  onClick={() => setZoom(1)}
                  className="px-2 py-1 text-xs rounded hover:bg-[#1D2230] text-[#98A4BC]"
                >
                  Reset
                </button>
              </div>
            </main>

            {/* Right Inspector & Layers Panel */}
            <aside className="w-80 border-l border-[#1D2230] bg-[#141721] flex flex-col shrink-0">
              {/* Layers Section Header */}
              <div className="p-3 border-b border-[#1D2230] flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Layers size={16} className="text-[#00E5FF]" />
                  <span className="font-semibold text-xs tracking-wider uppercase text-[#00E5FF]">Layers ({layers.length})</span>
                </div>
                {copiedLayerProps && (
                  <button
                    onClick={() => {
                      if (!selectedLayerId) return;
                      updateSelectedLayer(copiedLayerProps);
                    }}
                    className="text-[11px] bg-[#00E5FF]/20 text-[#00E5FF] px-2 py-0.5 rounded border border-[#00E5FF]/40 flex items-center gap-1 hover:bg-[#00E5FF]/30"
                  >
                    <Clipboard size={12} />
                    <span>Paste Props</span>
                  </button>
                )}
              </div>

              {/* Layer List */}
              <div className="flex-1 overflow-y-auto p-2 space-y-1.5 max-h-56">
                {layers.map(layer => {
                  const isSelected = layer.id === selectedLayerId;
                  return (
                    <div
                      key={layer.id}
                      onClick={() => setSelectedLayerId(layer.id)}
                      className={`flex items-center justify-between p-2 rounded-lg cursor-pointer transition border ${
                        isSelected 
                          ? 'bg-[#1D2230] border-[#00E5FF] text-white shadow' 
                          : 'bg-[#0C0E14] border-[#1D2230] text-[#98A4BC] hover:border-[#2E364A]'
                      }`}
                    >
                      <div className="flex items-center gap-2 truncate">
                        <div 
                          className="w-3.5 h-3.5 rounded-sm shrink-0" 
                          style={{ backgroundColor: layer.color }}
                        />
                        <span className="text-xs truncate font-medium">{layer.name}</span>
                      </div>
                      <div className="flex items-center gap-1 shrink-0">
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            setLayers(layers.map(l => l.id === layer.id ? { ...l, visible: !l.visible } : l));
                          }}
                          className="p-1 hover:text-white"
                        >
                          {layer.visible ? <Eye size={13} /> : <EyeOff size={13} className="text-red-400" />}
                        </button>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            setLayers(layers.map(l => l.id === layer.id ? { ...l, locked: !l.locked } : l));
                          }}
                          className="p-1 hover:text-white"
                        >
                          {layer.locked ? <Lock size={13} className="text-amber-400" /> : <Unlock size={13} />}
                        </button>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            setLayers(layers.filter(l => l.id !== layer.id));
                            if (selectedLayerId === layer.id) setSelectedLayerId(layers[0]?.id || '');
                          }}
                          className="p-1 hover:text-red-400"
                        >
                          <Trash2 size={13} />
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* 2D Transform & Properties Section (Section 3.7) */}
              {selectedLayer && (
                <div className="p-3 border-t border-[#1D2230] flex-1 overflow-y-auto space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-[#00E5FF] uppercase tracking-wider flex items-center gap-1.5">
                      <Move size={14} />
                      <span>2D Transform</span>
                    </span>
                    <button
                      onClick={() => {
                        setCopiedLayerProps({
                          scaleX: selectedLayer.scaleX,
                          scaleY: selectedLayer.scaleY,
                          rotation: selectedLayer.rotation,
                          opacity: selectedLayer.opacity
                        });
                      }}
                      className="text-[11px] text-[#98A4BC] hover:text-[#00E5FF] flex items-center gap-1"
                    >
                      <Copy size={12} />
                      <span>Copy Props</span>
                    </button>
                  </div>

                  {/* Position Move */}
                  <div className="space-y-1.5">
                    <div className="flex justify-between text-xs text-[#98A4BC]">
                      <span>Translation</span>
                      <span className="font-mono text-[#F0F4FC]">X: {selectedLayer.x.toFixed(0)} | Y: {selectedLayer.y.toFixed(0)}</span>
                    </div>
                    <div className="grid grid-cols-2 gap-2">
                      <input
                        type="range"
                        min="-200"
                        max="200"
                        value={selectedLayer.x}
                        onChange={(e) => updateSelectedLayer({ x: Number(e.target.value) })}
                        className="accent-[#00E5FF] h-1.5 bg-[#1D2230] rounded-lg cursor-pointer"
                      />
                      <input
                        type="range"
                        min="-200"
                        max="200"
                        value={selectedLayer.y}
                        onChange={(e) => updateSelectedLayer({ y: Number(e.target.value) })}
                        className="accent-[#00E5FF] h-1.5 bg-[#1D2230] rounded-lg cursor-pointer"
                      />
                    </div>
                  </div>

                  {/* Scale with Chain Toggle */}
                  <div className="space-y-1.5">
                    <div className="flex justify-between items-center text-xs text-[#98A4BC]">
                      <span>Scale (Chain Link)</span>
                      <button
                        onClick={() => updateSelectedLayer({ isScaleLinked: !selectedLayer.isScaleLinked })}
                        className={`text-[10px] px-1.5 py-0.5 rounded border ${selectedLayer.isScaleLinked ? 'bg-[#00E5FF]/20 text-[#00E5FF] border-[#00E5FF]/40' : 'bg-[#1D2230] text-[#98A4BC] border-[#2E364A]'}`}
                      >
                        {selectedLayer.isScaleLinked ? 'Linked' : 'Unlinked'}
                      </button>
                    </div>
                    {selectedLayer.isScaleLinked ? (
                      <input
                        type="range"
                        min="0.2"
                        max="3"
                        step="0.05"
                        value={selectedLayer.scaleX}
                        onChange={(e) => {
                          const val = Number(e.target.value);
                          updateSelectedLayer({ scaleX: val, scaleY: val });
                        }}
                        className="w-full accent-[#00E5FF] h-1.5 bg-[#1D2230] rounded-lg cursor-pointer"
                      />
                    ) : (
                      <div className="space-y-1">
                        <div className="flex justify-between text-[11px] text-[#98A4BC]">
                          <span>X: {(selectedLayer.scaleX * 100).toFixed(0)}%</span>
                          <span>Y: {(selectedLayer.scaleY * 100).toFixed(0)}%</span>
                        </div>
                        <input
                          type="range"
                          min="0.2"
                          max="3"
                          step="0.05"
                          value={selectedLayer.scaleX}
                          onChange={(e) => updateSelectedLayer({ scaleX: Number(e.target.value) })}
                          className="w-full accent-[#00E5FF] h-1.5 bg-[#1D2230] rounded-lg cursor-pointer"
                        />
                        <input
                          type="range"
                          min="0.2"
                          max="3"
                          step="0.05"
                          value={selectedLayer.scaleY}
                          onChange={(e) => updateSelectedLayer({ scaleY: Number(e.target.value) })}
                          className="w-full accent-[#FFB300] h-1.5 bg-[#1D2230] rounded-lg cursor-pointer"
                        />
                      </div>
                    )}
                  </div>

                  {/* Rotation */}
                  <div className="space-y-1.5">
                    <div className="flex justify-between text-xs text-[#98A4BC]">
                      <span>Rotation</span>
                      <span className="font-mono text-[#F0F4FC]">{selectedLayer.rotation}°</span>
                    </div>
                    <input
                      type="range"
                      min="0"
                      max="360"
                      value={selectedLayer.rotation}
                      onChange={(e) => updateSelectedLayer({ rotation: Number(e.target.value) })}
                      className="w-full accent-[#FFB300] h-1.5 bg-[#1D2230] rounded-lg cursor-pointer"
                    />
                  </div>

                  {/* Opacity */}
                  <div className="space-y-1.5">
                    <div className="flex justify-between text-xs text-[#98A4BC]">
                      <span>Opacity</span>
                      <span className="font-mono text-[#F0F4FC]">{(selectedLayer.opacity * 100).toFixed(0)}%</span>
                    </div>
                    <input
                      type="range"
                      min="0.1"
                      max="1"
                      step="0.05"
                      value={selectedLayer.opacity}
                      onChange={(e) => updateSelectedLayer({ opacity: Number(e.target.value) })}
                      className="w-full accent-[#00E5FF] h-1.5 bg-[#1D2230] rounded-lg cursor-pointer"
                    />
                  </div>
                </div>
              )}
            </aside>
          </>
        )}

        {/* Action Tree Tab (Section 3.10) */}
        {activeTab === 'action_tree' && (
          <div className="flex-1 p-6 overflow-y-auto space-y-6">
            <div>
              <h2 className="text-lg font-bold text-[#00E5FF] flex items-center gap-2">
                <GitBranch size={20} />
                <span>Action Tree Command Graph</span>
              </h2>
              <p className="text-sm text-[#98A4BC] mt-1">
                Headline feature: The history is a branching tree, not a line. Jump to any node across any branch to restore the canvas. Clipboard survives branch jumps.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {historyNodes.map(node => {
                const isActive = node.id === currentNodeId;
                return (
                  <div 
                    key={node.id}
                    onClick={() => setCurrentNodeId(node.id)}
                    className={`p-4 rounded-xl border cursor-pointer transition ${
                      isActive 
                        ? 'bg-[#1D2230] border-[#00E5FF] shadow-[0_0_15px_rgba(0,229,255,0.2)]' 
                        : 'bg-[#141721] border-[#1D2230] hover:border-[#2E364A]'
                    }`}
                  >
                    <div className="flex items-center justify-between text-xs mb-2">
                      <span className="font-mono px-2 py-0.5 rounded bg-[#0C0E14] text-[#00E5FF] border border-[#2E364A]">
                        {node.branch}
                      </span>
                      <span className="text-[#98A4BC]">{node.timestamp}</span>
                    </div>
                    <h3 className="font-semibold text-sm text-[#F0F4FC]">{node.title}</h3>
                    <div className="mt-3 flex items-center justify-between text-xs">
                      <span className="text-[#98A4BC]">{isActive ? 'Current Active State' : 'Tap to restore'}</span>
                      {isActive && <Check size={16} className="text-[#00E5FF]" />}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* Releases & APKs Tab */}
        {activeTab === 'build_status' && (
          <div className="flex-1 p-6 overflow-y-auto space-y-6 max-w-4xl">
            <div>
              <h2 className="text-lg font-bold text-[#00E5FF] flex items-center gap-2">
                <Smartphone size={20} />
                <span>GitHub Releases & Downloadable APKs</span>
              </h2>
              <p className="text-sm text-[#98A4BC] mt-1">
                Built automatically by GitHub Actions CI for Victor (MOTIONSALT). Installs directly on Android phones.
              </p>
            </div>

            <div className="p-4 rounded-xl bg-[#141721] border border-[#1D2230] space-y-3">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="font-bold text-white">Salty Graphics Beta (v1.0.0-beta1)</h3>
                  <p className="text-xs text-[#98A4BC]">Repository: motionssalt/salty-graphics</p>
                </div>
                <a
                  href="https://github.com/motionssalt/salty-graphics/releases"
                  target="_blank"
                  rel="noreferrer"
                  className="px-4 py-2 bg-[#00E5FF] text-black font-semibold rounded-lg text-xs flex items-center gap-1.5 hover:bg-[#6EFFFF] transition shadow"
                >
                  <Download size={14} />
                  <span>Download APKs</span>
                </a>
              </div>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-2 pt-2">
                <div className="p-2.5 bg-[#0C0E14] rounded-lg border border-[#1D2230]">
                  <div className="text-[11px] text-[#98A4BC]">Universal APK</div>
                  <div className="text-xs font-semibold text-white mt-0.5">All Devices</div>
                </div>
                <div className="p-2.5 bg-[#0C0E14] rounded-lg border border-[#1D2230]">
                  <div className="text-[11px] text-[#98A4BC]">arm64-v8a</div>
                  <div className="text-xs font-semibold text-[#00E5FF] mt-0.5">64-bit Phones</div>
                </div>
                <div className="p-2.5 bg-[#0C0E14] rounded-lg border border-[#1D2230]">
                  <div className="text-[11px] text-[#98A4BC]">armeabi-v7a</div>
                  <div className="text-xs font-semibold text-white mt-0.5">32-bit Phones</div>
                </div>
                <div className="p-2.5 bg-[#0C0E14] rounded-lg border border-[#1D2230]">
                  <div className="text-[11px] text-[#98A4BC]">x86_64</div>
                  <div className="text-xs font-semibold text-white mt-0.5">Emulators</div>
                </div>
              </div>
            </div>

            <div className="p-4 rounded-xl bg-[#141721] border border-[#1D2230] space-y-2">
              <h4 className="font-semibold text-sm text-[#00E5FF] flex items-center gap-2">
                <ShieldCheck size={16} />
                <span>Reference Keyflaw APK Preserved</span>
              </h4>
              <p className="text-xs text-[#98A4BC]">
                Saved permanently as GitHub pre-release asset <code>keyflaw-reference.apk</code> under tag <code>reference-keyflaw</code> per Section 6.
              </p>
              <a
                href="https://github.com/motionssalt/salty-graphics/releases/tag/reference-keyflaw"
                target="_blank"
                rel="noreferrer"
                className="text-xs text-[#00E5FF] hover:underline inline-flex items-center gap-1 mt-1"
              >
                <span>View Reference Release</span>
                <ExternalLink size={12} />
              </a>
            </div>
          </div>
        )}

        {/* Settings Tab */}
        {activeTab === 'settings' && (
          <div className="flex-1 p-6 overflow-y-auto space-y-6 max-w-3xl">
            <div>
              <h2 className="text-lg font-bold text-[#00E5FF] flex items-center gap-2">
                <Settings size={20} />
                <span>Settings & Contact Configuration</span>
              </h2>
              <p className="text-sm text-[#98A4BC] mt-1">
                Configured in a single file (<code>SocialConfig.kt</code>) as specified in Section 3.3.
              </p>
            </div>

            <div className="p-4 rounded-xl bg-[#141721] border border-[#1D2230] space-y-3">
              <h3 className="font-semibold text-sm text-white">Social & Direct Contacts</h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                <div className="p-2.5 bg-[#0C0E14] rounded-lg border border-[#1D2230]">
                  <span className="text-[#98A4BC]">WhatsApp:</span>
                  <div className="font-medium text-[#00E5FF] mt-0.5">09011565415</div>
                </div>
                <div className="p-2.5 bg-[#0C0E14] rounded-lg border border-[#1D2230]">
                  <span className="text-[#98A4BC]">Instagram / X / Threads:</span>
                  <div className="font-medium text-white mt-0.5">@motionssalt</div>
                </div>
                <div className="p-2.5 bg-[#0C0E14] rounded-lg border border-[#1D2230]">
                  <span className="text-[#98A4BC]">TikTok:</span>
                  <div className="font-medium text-white mt-0.5">@motionsalt</div>
                </div>
                <div className="p-2.5 bg-[#0C0E14] rounded-lg border border-[#1D2230]">
                  <span className="text-[#98A4BC]">Brand / Developer:</span>
                  <div className="font-medium text-white mt-0.5">Victor (MOTIONSALT)</div>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
